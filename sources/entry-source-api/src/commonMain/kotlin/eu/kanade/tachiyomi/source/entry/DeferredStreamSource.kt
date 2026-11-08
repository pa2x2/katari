package eu.kanade.tachiyomi.source.entry

/**
 * Optional capability for sources whose streams need extra requests before they can play.
 *
 * Such a source returns streams with a `null` [VideoStream.request] from [UnifiedSource.getMedia],
 * so listing them stays cheap. The host calls [resolveStream] only for the stream it is about to
 * play or download, and at most once per stream while the player stays open.
 */
interface DeferredStreamSource : UnifiedSource {

    /**
     * Returns [stream] with its [VideoStream.request] filled in. The result may also add
     * subtitles, audio tracks or markers that only the resolved stream knows about.
     * Throw when the stream can't be played; the host then moves to the next stream unless
     * the user picked this one.
     */
    suspend fun resolveStream(chapter: SEntryChapter, stream: VideoStream): VideoStream
}
