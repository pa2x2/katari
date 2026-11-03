package mihon.translation.provider.libretranslate.protocol

import mihon.translation.provider.server.call.ServerCallException
import mihon.translation.provider.server.call.ServerCallFailure
import mihon.translation.provider.server.call.ServerJsonCalls
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

internal interface LibreTranslateService {
    suspend fun languages(): List<LibreTranslateLanguage>

    suspend fun translate(
        text: String,
        source: String,
        target: String,
    ): String
}

internal class LibreTranslateHttpClient(
    httpClient: OkHttpClient,
    private val endpoint: HttpUrl,
    private val apiKey: String? = null,
) : LibreTranslateService {
    private val calls = ServerJsonCalls(httpClient)

    override suspend fun languages(): List<LibreTranslateLanguage> {
        val request = Request.Builder()
            .url(endpoint.newBuilder().addPathSegment("languages").build())
            .get()
            .build()
        val response = calls.answer<List<LibreTranslateLanguageResponse>>(request)
        return try {
            response.map { language ->
                LibreTranslateLanguage(
                    code = language.code,
                    name = language.name,
                    targets = language.targets.toSet(),
                )
            }
        } catch (_: IllegalArgumentException) {
            throw ServerCallException(ServerCallFailure.InvalidAnswer)
        }
    }

    override suspend fun translate(
        text: String,
        source: String,
        target: String,
    ): String {
        val payload = LibreTranslateRequest(
            q = text,
            source = source,
            target = target,
            apiKey = apiKey,
        )
        val request = Request.Builder()
            .url(endpoint.newBuilder().addPathSegment("translate").build())
            .post(calls.body(payload))
            .build()
        return calls.answer<LibreTranslateResponse>(request)
            .translatedText
            .takeIf(String::isNotBlank)
            ?: throw ServerCallException(ServerCallFailure.InvalidAnswer)
    }
}
