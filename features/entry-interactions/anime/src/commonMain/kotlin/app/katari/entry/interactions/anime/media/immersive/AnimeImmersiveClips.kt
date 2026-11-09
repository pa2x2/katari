package app.katari.entry.interactions.anime.media.immersive

import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.anime.state.resumePositionMs
import app.katari.entry.interactions.media.EntryImmersiveHandle
import app.katari.entry.interactions.media.EntryImmersiveRenderer
import app.katari.entry.interactions.runtime.EntryImmersiveLoadMode
import app.katari.video.engine.spi.VideoEngine
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.video.player.AnimePlayerBasePreferences
import eu.kanade.tachiyomi.ui.video.player.ResolveVideoStream
import eu.kanade.tachiyomi.ui.video.player.VideoStreamResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryProgressRepository

/**
 * Anime's clips in the immersive feed, alike on every platform: an episode's stream, resolved as the player resolves
 * it, played muted from where it was left in the platform's engine, with the [clipPlatform]'s input. A clip is a
 * preview: it follows the episode's saved place and never moves it. The feed's immersive processor is a platform
 * facility, so each platform answers it with its own `AnimeImmersiveProcessor` over these.
 */
internal class AnimeImmersiveClips(
    private val entryProgressRepository: EntryProgressRepository,
    private val resolveVideoStream: () -> VideoStreamResolver,
    private val videoEngine: () -> VideoEngine,
    private val basePreferences: () -> AnimePlayerBasePreferences,
    private val clipPlatform: AnimeClipPlatform,
) {
    val loadMode = EntryImmersiveLoadMode.FIRST_READING_CHILD

    // The clips beside the shown one are loaded and opened too, so moving to one starts it at once.
    val preloadRadius: Int = 1

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
            is ResolveVideoStream.Result.Success -> EntryImmersiveHandle.Playback(
                entryType = EntryType.ANIME,
                chapterId = chapter.id,
                stream = result.stream,
                delegate = AnimeImmersiveEpisode(chapter, result.preferences.subtitleKey),
            )
            is ResolveVideoStream.Result.Error -> error(result.reason.message())
        }
    }

    fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRenderer {
        val playback = handle as? EntryImmersiveHandle.Playback
            ?: error("Anime immersive feed received non-playback media")
        val episode = playback.delegate as? AnimeImmersiveEpisode
            ?: error("Anime immersive feed received media it didn't load")
        return AnimeImmersiveRenderer(
            handle = playback,
            savedPositionMs = savedPositionMs(episode.child),
            savedSubtitleKey = episode.subtitleKey,
            videoEngineProvider = videoEngine,
            basePreferencesProvider = basePreferences,
            platform = clipPlatform,
        )
    }

    /** Where the episode resumes, as the player and other devices save it. */
    private fun savedPositionMs(child: EntryChapter): Flow<Long> =
        entryProgressRepository.getByChapterIdAsFlow(child.id)
            .map { states ->
                states.firstOrNull { it.contentKey.isEmpty() && it.resourceKey == child.progressResourceKey }
                    ?.resumePositionMs
                    ?: 0L
            }
            .distinctUntilChanged()
}

// The title's subtitle pick, which the clip shows as the player would
private data class AnimeImmersiveEpisode(val child: EntryChapter, val subtitleKey: String?)

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
        ResolveVideoStream.Reason.PickNotOffered -> "The source does not offer this dub or server for this episode"
        is ResolveVideoStream.Reason.StreamFetchFailed -> cause.message ?: "Unable to resolve video stream"
    }
}
