package eu.kanade.tachiyomi.source.entry

import mihon.book.api.BookContentDescriptor

/**
 * Sealed media payload returned by [UnifiedSource.getMedia].
 *
 * The player/reader consumes this value and dispatches to the appropriate renderer.
 */
sealed interface EntryMedia {

    /**
     * Image-based media for manga.
     *
     * @property pages ordered image descriptors consumed by the reader.
     */
    data class ImagePages(val pages: List<EntryImagePage>) : EntryMedia

    /**
     * Playback media for anime. The descriptor contains the resolved selection
     * and playable streams; the player picks the best matching stream.
     *
     * @property descriptor resolved playback options and streams.
     */
    data class Playback(val descriptor: PlaybackDescriptor) : EntryMedia

    /**
     * Readable book content. The payload describes source-owned resources and
     * data-only access locations; format processors never call the source directly.
     *
     * @property descriptor open format, profile, and protection identifiers used for processor selection.
     * @property publicationKeyOverride optional discriminator when one entry contains multiple publications.
     * @property catalog bounded snapshot of source-known resources.
     * @property hierarchy optional entry-screen grouping hints.
     * @property initialResourceId optional resource selected by this source child.
     * @property initialResourceLocation resolved location for [initialResourceId], when known.
     */
    data class Book(
        val descriptor: BookContentDescriptor,
        val publicationKeyOverride: String? = null,
        val catalog: BookResourceCatalog = BookResourceCatalog(),
        val hierarchy: List<BookResourceHierarchyNode> = emptyList(),
        val initialResourceId: String? = null,
        val initialResourceLocation: BookResourceLocation? = null,
    ) : EntryMedia {
        init {
            require(publicationKeyOverride == null || publicationKeyOverride.isNotBlank()) {
                "publication key override must not be blank"
            }
            require(initialResourceId == null || initialResourceId.isNotBlank()) {
                "initial resource id must not be blank"
            }
            require(initialResourceLocation == null || initialResourceId != null) {
                "an initial resource location requires an initial resource id"
            }
        }
    }
}
