package mihon.entry.interactions.anime

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.video.player.ResolveVideoStream
import eu.kanade.tachiyomi.ui.video.player.VideoPlaybackSession
import eu.kanade.tachiyomi.ui.video.player.VideoStreamResolver
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.entry.interactions.EntryImmersiveFeedHandle
import mihon.entry.interactions.EntryImmersiveFeedProcessor
import mihon.entry.interactions.EntryImmersiveFeedProgress
import mihon.entry.interactions.EntryImmersiveFeedRenderer
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.history.repository.HistoryRepository

internal class AnimeImmersiveFeedProcessor(
    private val entryProgressRepository: EntryProgressRepository,
    private val historyRepository: HistoryRepository?,
    private val resolveVideoStream: () -> VideoStreamResolver,
) : EntryImmersiveFeedProcessor {
    override val type: EntryType = EntryType.ANIME
    private val persistMutex = Mutex()

    override fun isSupported(entry: Entry): Boolean = entry.type == type

    override fun preloadRadius(entryType: EntryType): Int = 1

    override suspend fun load(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        source: UnifiedSource,
    ): EntryImmersiveFeedHandle {
        return when (
            val result = resolveVideoStream()(
                entryId = entry.id,
                chapterId = chapter.id,
                ownerEntryId = entry.id,
            )
        ) {
            is ResolveVideoStream.Result.Success -> {
                val progress = entryProgressRepository.get(entry.id, "", chapter.url)
                val session = VideoPlaybackSession(entry.id, chapter.id, chapter.url).apply {
                    restore(progress)
                }
                EntryImmersiveFeedHandle.Playback(
                    entryType = type,
                    chapterId = chapter.id,
                    stream = result.stream,
                    subtitles = result.subtitles,
                    resumePositionMs = progress?.positionMs ?: 0L,
                    delegate = session,
                )
            }
            is ResolveVideoStream.Result.Error -> error(result.reason.message())
        }
    }

    override fun renderer(handle: EntryImmersiveFeedHandle): EntryImmersiveFeedRenderer {
        val playback = handle as? EntryImmersiveFeedHandle.Playback
            ?: error("Anime immersive feed received non-playback media")
        return AnimeImmersiveFeedRenderer(playback)
    }

    override suspend fun persistProgress(
        handle: EntryImmersiveFeedHandle,
        progress: EntryImmersiveFeedProgress,
    ) {
        val playback = handle as? EntryImmersiveFeedHandle.Playback ?: return
        val playbackProgress = progress as? EntryImmersiveFeedProgress.Playback ?: return
        val session = playback.delegate as? VideoPlaybackSession ?: return
        persistMutex.withLock {
            val snapshot = session.snapshot(
                positionMs = playbackProgress.positionMs,
                durationMs = playbackProgress.durationMs,
            )
            entryProgressRepository.mergeAndSyncChild(snapshot.progressState)
            snapshot.historyUpdate?.let { historyRepository?.upsertHistory(it) }
            if (playbackProgress.resetSession) session.restore(0L)
        }
    }

    override fun release(handle: EntryImmersiveFeedHandle) = Unit
}

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
