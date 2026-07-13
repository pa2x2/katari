package eu.kanade.tachiyomi.source.entry

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
}
