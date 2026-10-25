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
}
