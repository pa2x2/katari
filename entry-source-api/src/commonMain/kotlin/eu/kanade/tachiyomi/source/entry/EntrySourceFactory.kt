package eu.kanade.tachiyomi.source.entry

/**
 * Factory for Entry-era extensions that expose multiple unified sources.
 */
interface EntrySourceFactory {

    fun createSources(): List<UnifiedSource>
}
