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
     * Whether the source opts in to immersive catalogue and feed browsing.
     *
     * Sources that return `true` advertise that their content can be consumed inline through
     * immersive browsing. Defaults to `false`; sources must explicitly opt in.
     */
    val supportsImmersiveFeed: Boolean
        get() = false
}
