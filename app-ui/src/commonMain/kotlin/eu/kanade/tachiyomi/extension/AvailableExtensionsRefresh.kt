package eu.kanade.tachiyomi.extension

/** Reads again what the extension stores offer, once a store was added or removed. */
fun interface AvailableExtensionsRefresh {
    suspend fun refresh()
}
