package eu.kanade.tachiyomi.source.entry

/**
 * Optional capability for sources that can expose external subtitle tracks.
 */
interface SubtitleSource : UnifiedSource {

    /**
     * Resolve external subtitle tracks for a chapter and selection.
     */
    suspend fun getSubtitles(
        chapter: SEntryChapter,
        selection: PlaybackSelection = PlaybackSelection(),
    ): List<VideoSubtitle>
}
