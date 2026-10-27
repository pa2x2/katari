package eu.kanade.tachiyomi.source.entry

/**
 * A [UnifiedSource] that participates in catalogue browsing.
 *
 * This is distinct from legacy compatibility source contracts.
 */
interface EntryCatalogueSource : UnifiedSource {

    /**
     * An ISO 639-1 compliant language code (two letters in lower case).
     */
    val lang: String

    /**
     * Whether the source has support for latest updates.
     */
    val supportsLatest: Boolean

    /**
     * Whether a text query is searched together with the filters. False for a source whose search box, like many
     * sites', ignores them; Katari then shows the filters as unused while a query is typed.
     */
    val textSearchUsesFilters: Boolean
        get() = true
}
