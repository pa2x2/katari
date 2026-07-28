package mihon.translation.provider.libretranslate.protocol

import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

    private suspend fun execute(request: Request): String {
        val response = try {
            httpClient.newCall(request).await()
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            throw LibreTranslateException(LibreTranslateFailureKind.Connection)
        }
        response.use {
            if (!it.isSuccessful) {
                throw LibreTranslateException(
                    when (it.code) {
                        in 400..499 -> LibreTranslateFailureKind.Rejected
                        else -> LibreTranslateFailureKind.Server
                    },
                )
            }
            return it.body.string()
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
