package mihon.translation.provider.libretranslate.protocol

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

internal interface LibreTranslateService {
    suspend fun languages(): List<LibreTranslateLanguage>

    suspend fun translate(
        text: String,
        source: String,
        target: String,
    ): String
}

internal class LibreTranslateHttpClient(
    private val httpClient: OkHttpClient,
    private val endpoint: HttpUrl,
    private val apiKey: String? = null,
    private val json: Json = DEFAULT_JSON,
) : LibreTranslateService {
    override suspend fun languages(): List<LibreTranslateLanguage> {
        val request = Request.Builder()
            .url(endpoint.newBuilder().addPathSegment("languages").build())
            .get()
            .build()
        val body = execute(request)
        val response = decode<List<LibreTranslateLanguageResponse>>(body)
        return try {
            response.map { language ->
                LibreTranslateLanguage(
                    code = language.code,
                    name = language.name,
                    targets = language.targets.toSet(),
                )
            }
        } catch (_: IllegalArgumentException) {
            throw LibreTranslateException(LibreTranslateFailureKind.InvalidResponse)
        }
    }

    override suspend fun translate(
        text: String,
        source: String,
        target: String,
    ): String {
        val payload = json.encodeToString(
            LibreTranslateRequest(
                q = text,
                source = source,
                target = target,
                apiKey = apiKey,
            ),
        )
        val request = Request.Builder()
            .url(endpoint.newBuilder().addPathSegment("translate").build())
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return decode<LibreTranslateResponse>(execute(request))
            .translatedText
            .takeIf(String::isNotBlank)
            ?: throw LibreTranslateException(LibreTranslateFailureKind.InvalidResponse)
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
                            Result.failure(LibreTranslateException(LibreTranslateFailureKind.Connection)),
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
            throw LibreTranslateException(
                when (response.code) {
                    in 400..499 -> LibreTranslateFailureKind.Rejected
                    else -> LibreTranslateFailureKind.Server
                },
            )
        }
        return try {
            response.body.string()
        } catch (_: IOException) {
            throw LibreTranslateException(LibreTranslateFailureKind.Connection)
        }
    }

    private inline fun <reified T> decode(body: String): T {
        return try {
            json.decodeFromString(body)
        } catch (_: SerializationException) {
            throw LibreTranslateException(LibreTranslateFailureKind.InvalidResponse)
        } catch (_: IllegalArgumentException) {
            throw LibreTranslateException(LibreTranslateFailureKind.InvalidResponse)
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
