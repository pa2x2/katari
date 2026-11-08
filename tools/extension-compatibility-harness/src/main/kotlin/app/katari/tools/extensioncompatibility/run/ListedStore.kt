package app.katari.tools.extensioncompatibility.run

import app.katari.desktop.extension.ExtensionHostGraph
import app.katari.domain.extension.model.ExtensionStore
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/** A store of the list as the host read it: its listings, or why it couldn't be read. */
internal data class ListedStore(
    /** As the store list gives it, which the host may have moved to a newer index ([ExtensionStore.indexUrl]). */
    val url: String,
    val store: ExtensionStore?,
    val error: String?,
    val listings: List<Extension.Available>,
)

/**
 * Adds [urls] as the extension screens add a store, then reads what they list through the extension manager, which
 * keeps the newest listing of each extension the way the screens show it.
 */
internal suspend fun readStores(host: ExtensionHostGraph, urls: List<String>): List<ListedStore> {
    val fetched = urls.map { url -> url to host.extensionStoreRepository.fetch(url) }
    fetched.forEach { (_, result) -> result.onSuccess { host.extensionStoreRepository.insert(it) } }
    host.extensionManager.findAvailableExtensions()
    // The listings are shared while someone collects them and start out empty, so the first value can be that one
    val available = withTimeoutOrNull(LISTINGS_TIMEOUT) {
        host.extensionManager.availableExtensionsFlow.first { it.isNotEmpty() }
    }.orEmpty()
    return fetched.map { (url, result) ->
        result.fold(
            onSuccess = { store ->
                ListedStore(url, store, null, available.filter { it.store.indexUrl == store.indexUrl })
            },
            onFailure = { error ->
                ListedStore(url, null, error.message ?: error::class.java.name, emptyList())
            },
        )
    }
}

private val LISTINGS_TIMEOUT = 30.seconds
