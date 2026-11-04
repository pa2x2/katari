package mihon.entry.interactions.anime.media.immersive

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.video.player.AnimePlayerBasePreferences
import eu.kanade.tachiyomi.ui.video.player.ResolveVideoStream
import eu.kanade.tachiyomi.ui.video.player.VideoPlaybackSession
import eu.kanade.tachiyomi.ui.video.player.VideoStreamResolver
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.core.platform.PlatformContext
import mihon.entry.interactions.anime.state.positionMs
import mihon.entry.interactions.media.EntryImmersiveHandle
import mihon.entry.interactions.media.EntryImmersiveProgress
import mihon.entry.interactions.media.EntryImmersiveRenderer
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.runtime.EntryImmersiveLoadMode
import mihon.video.engine.spi.VideoEngine
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryProgressRepository

/**
 * Anime's clips in the immersive feed, alike on every platform: an episode's stream, resolved as the player resolves
 * it, played muted from where it was left in the platform's engine, with the [clipPlatform]'s input. The feed's
 * immersive processor is a platform facility, so each platform answers it with its own `AnimeImmersiveProcessor`
 * over these.
 */
internal class AnimeImmersiveClips(
    private val entryProgressRepository: EntryProgressRepository,
    private val resolveVideoStream: () -> VideoStreamResolver,
    private val mediaSession: EntryMediaSessionProcessor,
    private val videoEngine: () -> VideoEngine,
    private val basePreferences: () -> AnimePlayerBasePreferences,
    private val clipPlatform: AnimeClipPlatform,
) {
    val loadMode = EntryImmersiveLoadMode.FIRST_READING_CHILD

    // The clips beside the shown one are loaded and opened too, so moving to one starts it at once.
    val preloadRadius: Int = 1
    private val persistMutex = Mutex()

    suspend fun load(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
    ): EntryImmersiveHandle {
        requireNotNull(chapter) { "Anime immersive loading requires a reading child" }
        val resolver = resolveVideoStream()
        val listed = resolver(entryId = entry.id, chapterId = chapter.id, ownerEntryId = entry.id)
        val result = when (listed) {
            is ResolveVideoStream.Result.Success -> resolver.resolveLink(
                listed,
                resolvedStreams = null,
                explicitPick = false,
            )
            is ResolveVideoStream.Result.Error -> listed
        }
        return when (result) {
            is ResolveVideoStream.Result.Success -> {
                val progress = entryProgressRepository.get(entry.id, "", chapter.progressResourceKey)
                val session = VideoPlaybackSession(entry.id, chapter.id, chapter.progressResourceKey).apply {
                    restore(progress)
                }
                EntryImmersiveHandle.Playback(
                    entryType = EntryType.ANIME,
                    chapterId = chapter.id,
                    stream = result.stream,
                    resumePositionMs = progress?.positionMs ?: 0L,
                    delegate = AnimeImmersiveSession(entry, chapter, session),
                )
            }
            is ResolveVideoStream.Result.Error -> error(result.reason.message())
        }
    }

    fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRenderer {
        val playback = handle as? EntryImmersiveHandle.Playback
            ?: error("Anime immersive feed received non-playback media")
        return AnimeImmersiveRenderer(playback, videoEngine, basePreferences, clipPlatform)
    }

    suspend fun persistProgress(
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
                activeDurationMs = playbackProgress.activeDurationMs,
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
                    activity = snapshot.activity,
                ),
            )
            if (playbackProgress.resetSession) immersiveSession.playback.restore(0L)
        }
    }
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
        ResolveVideoStream.Reason.TorrentOnly -> "Only offered as a torrent"
        ResolveVideoStream.Reason.StreamFetchTimeout -> "Timed out while resolving streams"
        ResolveVideoStream.Reason.OfflineNoDownload -> "Device is offline and this episode is not downloaded"
        is ResolveVideoStream.Reason.StreamFetchFailed -> cause.message ?: "Unable to resolve video stream"
    }
}
