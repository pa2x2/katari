package eu.kanade.tachiyomi.source.entry

/**
 * Factory for Entry-era extensions that expose multiple unified sources.
 */
interface EntrySourceFactory {

    /** Creates every source exposed by this extension. */
    fun createSources(): List<UnifiedSource>
}
