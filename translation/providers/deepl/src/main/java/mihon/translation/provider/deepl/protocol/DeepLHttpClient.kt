package mihon.translation.provider.deepl.protocol

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

internal interface DeepLService {
    suspend fun languages(): DeepLLanguages

    /** Translates [texts] together, answering each in order; [context] is read but not translated. */
    suspend fun translate(
        texts: List<String>,
        source: String,
        target: String,
        context: String?,
    ): List<String>
}

/** Speaks the part of the DeepL API v2 that translating text needs, to DeepL or to a server compatible with it. */
internal class DeepLHttpClient(
    private val httpClient: OkHttpClient,
    private val endpoint: HttpUrl,
    private val apiKey: String? = null,
    private val json: Json = DEFAULT_JSON,
) : DeepLService {
    override suspend fun languages(): DeepLLanguages =
        DeepLLanguages(sources = languages("source"), targets = languages("target"))

    private suspend fun languages(type: String): List<DeepLLanguage> {
        val request = request("languages") { addQueryParameter("type", type) }.get().build()
        return decode<List<DeepLLanguageResponse>>(execute(request))
            .filter { it.language.isNotBlank() }
            .map { DeepLLanguage(code = it.language, name = it.name) }
    }

    override suspend fun translate(
        texts: List<String>,
        source: String,
        target: String,
        context: String?,
    ): List<String> {
        val payload = json.encodeToString(
            DeepLTranslateRequest(
                text = texts,
                sourceLanguage = source,
                targetLanguage = target,
                context = context,
            ),
        )
        val request = request("translate").post(payload.toRequestBody(JSON_MEDIA_TYPE)).build()
        return decode<DeepLTranslateResponse>(execute(request)).translations.map(DeepLTranslation::text)
    }

    private fun request(path: String, url: HttpUrl.Builder.() -> Unit = {}): Request.Builder {
        val builder = Request.Builder()
            .url(endpoint.newBuilder().addPathSegment("v2").addPathSegment(path).apply(url).build())
        apiKey?.let { builder.header("Authorization", "DeepL-Auth-Key $it") }
        return builder
    }

    /**
     * Keeps the call cancellable until its body has been read, so cancelling a translation aborts
     * a slow response body instead of waiting for the socket read timeout.
     */
    private suspend fun execute(request: Request): String {
        val call = httpClient.newCall(request)
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWith(
                            Result.failure(DeepLException(DeepLFailureKind.Connection, e)),
                        )
                    }

                    override fun onResponse(call: Call, response: Response) {
                        continuation.resumeWith(runCatching { response.use(::readBody) })
                    }
                },
            )
        }
    }

    private fun readBody(response: Response): String {
        if (!response.isSuccessful) {
            throw DeepLException(
                when (response.code) {
                    401, 403 -> DeepLFailureKind.Unauthorized
                    QUOTA_EXCEEDED -> DeepLFailureKind.QuotaExceeded
                    in 400..499 -> DeepLFailureKind.Rejected
                    else -> DeepLFailureKind.Server
                },
            )
        }
        return try {
            response.body.string()
        } catch (e: IOException) {
            throw DeepLException(DeepLFailureKind.Connection, e)
        }
    }

    private inline fun <reified T> decode(body: String): T {
        return try {
            json.decodeFromString(body)
        } catch (_: SerializationException) {
            throw DeepLException(DeepLFailureKind.InvalidResponse)
        } catch (_: IllegalArgumentException) {
            throw DeepLException(DeepLFailureKind.InvalidResponse)
        }
    }

    private companion object {
        /** DeepL's status for a used-up character quota. */
        const val QUOTA_EXCEEDED = 456
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
