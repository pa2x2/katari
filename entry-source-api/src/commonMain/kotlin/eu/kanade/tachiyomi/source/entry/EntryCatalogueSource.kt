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
     * Whether the source opts in to the immersive feed view.
     *
     * Sources that return `true` advertise that their content can be consumed
     * inline through the feed's immersive view. Defaults to `false`; sources
     * must explicitly opt in.
     */
    val supportsImmersiveFeed: Boolean
        get() = false
}
