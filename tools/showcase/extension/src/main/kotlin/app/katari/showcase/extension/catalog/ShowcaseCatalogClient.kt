package app.katari.showcase.extension.catalog

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Headers
import okhttp3.OkHttpClient

/** Loads `catalog.json` from the content server once and shares it between the showcase sources. */
internal class ShowcaseCatalogClient(private val baseUrl: String) {

    private val mutex = Mutex()
    private var catalog: ShowcaseCatalog? = null

    suspend fun catalog(client: OkHttpClient, headers: Headers): ShowcaseCatalog = mutex.withLock {
        catalog ?: client.newCall(GET("$baseUrl/catalog.json", headers)).awaitSuccess().use { response ->
            showcaseJson.decodeFromString<ShowcaseCatalog>(response.body.string())
        }.also { catalog = it }
    }
}
