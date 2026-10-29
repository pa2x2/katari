package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.VideoStream

/**
 * Streams resolved while one player stays open, so going back to a stream, dub or episode doesn't ask the source again.
 * Resolved links expire, so nothing outlives the player and nothing is written to disk.
 */
internal class ResolvedStreamCache {

    private val streams = HashMap<Key, VideoStream>()

    /** [stream] as resolved before for this episode and selection, else resolved now by [resolve]. */
    suspend fun getOrResolve(
        chapterId: Long,
        selection: PlaybackSelection,
        stream: VideoStream,
        resolve: suspend (VideoStream) -> VideoStream,
    ): VideoStream {
        val key = Key(chapterId, selection.copy(streamKey = null), streamChoiceKey(stream))
        synchronized(streams) { streams[key] }?.let { return it }
        return resolve(stream).also { resolved -> synchronized(streams) { streams[key] = resolved } }
    }

    /** Forgets every resolved link, for when one stopped working and the others may have expired with it. */
    fun clear() {
        synchronized(streams) { streams.clear() }
    }

    // The same stream key names different videos in another episode, dub or server.
    private data class Key(val chapterId: Long, val selection: PlaybackSelection, val streamKey: String)
}
