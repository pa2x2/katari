package mihon.entry.interactions.anime

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.video.player.ResolveVideoStream
import eu.kanade.tachiyomi.ui.video.player.VideoPlaybackSession
import eu.kanade.tachiyomi.ui.video.player.VideoStreamResolver
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.entry.interactions.EntryImmersiveHandle
import mihon.entry.interactions.EntryImmersiveLoadMode
import mihon.entry.interactions.EntryImmersiveProcessor
import mihon.entry.interactions.EntryImmersiveProgress
import mihon.entry.interactions.EntryImmersiveRenderer
import mihon.entry.interactions.EntryMediaSessionActivity
import mihon.entry.interactions.EntryMediaSessionEvent
import mihon.entry.interactions.EntryMediaSessionProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class AnimeImmersiveProcessor(
    private val entryProgressRepository: EntryProgressRepository,
    private val resolveVideoStream: () -> VideoStreamResolver,
    private val mediaSession: EntryMediaSessionProcessor,
) : EntryImmersiveProcessor {
    override val type: EntryType = EntryType.ANIME
    override val loadMode = EntryImmersiveLoadMode.FIRST_READING_CHILD
    override val preloadRadius: Int = 1
    private val persistMutex = Mutex()

    override suspend fun load(
        context: Context,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
    ): EntryImmersiveHandle {
        requireNotNull(chapter) { "Anime immersive loading requires a reading child" }
        return when (
            val result = resolveVideoStream()(
                entryId = entry.id,
                chapterId = chapter.id,
                ownerEntryId = entry.id,
            )
        ) {
            is ResolveVideoStream.Result.Success -> {
                val progress = entryProgressRepository.get(entry.id, "", chapter.progressResourceKey)
                val session = VideoPlaybackSession(entry.id, chapter.id, chapter.progressResourceKey).apply {
                    restore(progress)
                }
                EntryImmersiveHandle.Playback(
                    entryType = type,
                    chapterId = chapter.id,
                    stream = result.stream,
                    subtitles = result.subtitles,
                    resumePositionMs = progress?.positionMs ?: 0L,
                    delegate = AnimeImmersiveSession(entry, chapter, session),
                )
            }
            is ResolveVideoStream.Result.Error -> error(result.reason.message())
        }
    }

    override fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRenderer {
        val playback = handle as? EntryImmersiveHandle.Playback
            ?: error("Anime immersive feed received non-playback media")
        return AnimeImmersiveRenderer(playback)
    }

    override suspend fun persistProgress(
        handle: EntryImmersiveHandle,
        progress: EntryImmersiveProgress,
    ) {
        val playback = handle as? EntryImmersiveHandle.Playback ?: return
        val playbackProgress = progress as? EntryImmersiveProgress.Playback ?: return
        val immersiveSession = playback.delegate as? AnimeImmersiveSession ?: return
        persistMutex.withLock {
            val snapshot = immersiveSession.playback.snapshot(
                positionMs = playbackProgress.positionMs,
                durationMs = playbackProgress.durationMs,
            )
            mediaSession.onEvent(
                EntryMediaSessionEvent.Progressed(
                    visibleEntry = immersiveSession.entry,
                    child = immersiveSession.child,
                    progress = snapshot.progressState,
                    fraction = if (playbackProgress.durationMs > 0L) {
                        playbackProgress.positionMs.toDouble() / playbackProgress.durationMs
                    } else {
                        0.0
                    },
                    activity = snapshot.historyUpdate?.let { history ->
                        EntryMediaSessionActivity(
                            recordedAtEpochMillis = history.readAt.time,
                            durationMillis = history.sessionReadDuration,
                        )
                    },
                ),
            )
            if (playbackProgress.resetSession) immersiveSession.playback.restore(0L)
        }
    }

    override fun release(handle: EntryImmersiveHandle) = Unit
}

private data class AnimeImmersiveSession(
    val entry: Entry,
    val child: EntryChapter,
    val playback: VideoPlaybackSession,
)

private fun ResolveVideoStream.Reason.message(): String {
    return when (this) {
        ResolveVideoStream.Reason.VideoNotFound -> "Video not found"
        ResolveVideoStream.Reason.EpisodeNotFound -> "Episode not found"
        ResolveVideoStream.Reason.EpisodeMismatch -> "Episode does not belong to this entry"
        ResolveVideoStream.Reason.SourceLoadTimeout -> "Video source took too long to load"
        ResolveVideoStream.Reason.SourceNotFound -> "Video source not available"
        ResolveVideoStream.Reason.NoStreams -> "No playable streams returned"
        ResolveVideoStream.Reason.StreamFetchTimeout -> "Timed out while resolving streams"
        ResolveVideoStream.Reason.OfflineNoDownload -> "Device is offline and this episode is not downloaded"
        is ResolveVideoStream.Reason.StreamFetchFailed -> cause.message ?: "Unable to resolve video stream"
    }
}
