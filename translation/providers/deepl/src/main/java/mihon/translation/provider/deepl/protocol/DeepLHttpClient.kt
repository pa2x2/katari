package mihon.translation.provider.deepl.protocol

import mihon.translation.provider.server.call.ServerJsonCalls
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

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
    httpClient: OkHttpClient,
    private val endpoint: HttpUrl,
    private val apiKey: String? = null,
) : DeepLService {
    private val calls = ServerJsonCalls(httpClient, retries = BUSY_RETRIES)

    override suspend fun languages(): DeepLLanguages =
        DeepLLanguages(sources = languages("source"), targets = languages("target"))

    private suspend fun languages(type: String): List<DeepLLanguage> {
        val request = request("languages") { addQueryParameter("type", type) }.get().build()
        return calls.answer<List<DeepLLanguageResponse>>(request)
            .filter { it.language.isNotBlank() }
            .map { DeepLLanguage(code = it.language, name = it.name) }
    }

    override suspend fun translate(
        texts: List<String>,
        source: String,
        target: String,
        context: String?,
    ): List<String> {
        val payload = DeepLTranslateRequest(
            text = texts,
            sourceLanguage = source,
            targetLanguage = target,
            context = context,
        )
        val request = request("translate").post(calls.body(payload)).build()
        return calls.answer<DeepLTranslateResponse>(request).translations.map(DeepLTranslation::text)
    }

    private fun request(path: String, url: HttpUrl.Builder.() -> Unit = {}): Request.Builder {
        val builder = Request.Builder()
            .url(endpoint.newBuilder().addPathSegment("v2").addPathSegment(path).apply(url).build())
        apiKey?.let { builder.header("Authorization", "DeepL-Auth-Key $it") }
        return builder
    }

    private companion object {
        /** DeepL limits how often it is asked, and asks to be asked again when it is. */
        const val BUSY_RETRIES = 3
    }
}
