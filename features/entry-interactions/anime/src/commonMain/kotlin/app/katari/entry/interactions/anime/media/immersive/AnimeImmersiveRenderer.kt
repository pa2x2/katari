package app.katari.entry.interactions.anime.media.immersive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleStartEffect
import app.katari.entry.interactions.media.EntryImmersiveActiveSessionEffect
import app.katari.entry.interactions.media.EntryImmersiveHandle
import app.katari.entry.interactions.media.EntryImmersiveMediaSwitch
import app.katari.entry.interactions.media.EntryImmersiveMediaSwitches
import app.katari.entry.interactions.media.EntryImmersivePlace
import app.katari.entry.interactions.media.EntryImmersivePlaceTracker
import app.katari.entry.interactions.media.EntryImmersiveRenderer
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Subtitles
import app.katari.video.engine.spi.VideoEngine
import app.katari.video.engine.spi.VideoPlayerRole
import app.katari.video.engine.spi.loading.resolvedRequest
import app.katari.video.engine.spi.player.VideoPlayer
import app.katari.video.engine.spi.player.VideoPlayerEvent
import app.katari.video.engine.spi.player.VideoPlayerState
import eu.kanade.tachiyomi.ui.video.player.AnimePlayerBasePreferences
import eu.kanade.tachiyomi.ui.video.player.mediacontrols.VideoMetadata
import eu.kanade.tachiyomi.ui.video.player.playback.capturePlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.playback.playbackSnapshot
import kotlinx.coroutines.flow.Flow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * A clip in the immersive feed. It starts where the episode was left and follows that place when the player or another
 * device moves it, but never saves a place of its own: watching in the feed is a preview.
 */
internal class AnimeImmersiveRenderer(
    private val handle: EntryImmersiveHandle.Playback,
    private val savedPositionMs: Flow<Long>,
    private val savedSubtitleKey: String?,
    private val metadata: VideoMetadata,
    private val videoEngineProvider: () -> VideoEngine,
    private val basePreferencesProvider: () -> AnimePlayerBasePreferences,
    private val platform: AnimeClipPlatform,
) : EntryImmersiveRenderer {

    @Composable
    override fun Content(
        modifier: Modifier,
        active: Boolean,
        controlsVisible: Boolean,
        controlsBottomInset: Dp,
        onToggleControls: () -> Unit,
        onPagingBlockedChange: (Boolean) -> Unit,
        place: EntryImmersivePlaceTracker,
        onMediaWidthChange: (Dp?) -> Unit,
        switches: EntryImmersiveMediaSwitches,
    ) {
        val videoEngine = remember { videoEngineProvider() }
        val preferences = remember { basePreferencesProvider() }
        val unknownError = stringResource(MR.strings.unknown_error)
        var playerErrorMessage by remember(handle.chapterId, handle.stream.resolvedRequest.url) {
            mutableStateOf<String?>(null)
        }
        var hasRenderedFirstFrame by remember(handle.chapterId, handle.stream.resolvedRequest.url) {
            mutableStateOf(false)
        }
        val player = remember(handle.chapterId, handle.stream.resolvedRequest.url) {
            videoEngine.createPlayer(
                stream = handle.stream,
                role = VideoPlayerRole.Feed,
            ).also { player ->
                player.setRepeatOne(true)
                player.addListener { event ->
                    when (event) {
                        VideoPlayerEvent.FirstFrameRendered -> hasRenderedFirstFrame = true
                        is VideoPlayerEvent.Failed -> playerErrorMessage = event.error.message ?: unknownError
                        is VideoPlayerEvent.PositionJumped -> Unit
                    }
                }
            }
        }
        val playerState by player.state.collectAsState()
        val isBuffering = playerState.playback == VideoPlayerState.Playback.Buffering
        // Buffering counts as playing so a stall doesn't let the screen time out mid-clip.
        val keepAwake = playerState.playWhenReady &&
            playerState.playback != VideoPlayerState.Playback.Idle &&
            playerState.playback != VideoPlayerState.Playback.Ended
        var playbackSnapshot by remember(player) { mutableStateOf(player.capturePlaybackSnapshot()) }
        var playIntent by remember(player) { mutableStateOf(true) }
        var muted by remember(player) { mutableStateOf(preferences.immersiveFeedMuted) }
        val latestPlayIntent by rememberUpdatedState(playIntent)
        val latestActive by rememberUpdatedState(active)

        LaunchedEffect(active) {
            if (active) onPagingBlockedChange(false)
        }
        LaunchedEffect(player) {
            var followed: Long? = null
            savedPositionMs.collect { saved ->
                if (followed == null) {
                    if (saved > 0L) player.seekTo(saved)
                    player.setPlayWhenReady(latestActive)
                    player.prepare()
                } else {
                    player.seekTo(saved)
                }
                followed = saved
            }
        }
        val volumePercent = platform.playerVolume?.collectAsState()?.value
        LaunchedEffect(player, active, muted, volumePercent) {
            if (active) {
                val storedMuted = preferences.immersiveFeedMuted
                if (storedMuted != muted) {
                    muted = storedMuted
                    return@LaunchedEffect
                }
            }
            player.setVolume(if (active && !muted) clipVolume(volumePercent) else 0f)
            volumePercent?.let { player.setVolumeBoost(it.coerceAtLeast(VideoPlayer.NO_VOLUME_BOOST_PERCENT)) }
            if (active && playIntent) {
                player.play()
            } else if (!active) {
                player.pause()
            }
        }
        platform.UnmuteOnVolumeRaise(active = active, muted = muted) {
            muted = false
            preferences.immersiveFeedMuted = false
        }
        LifecycleStartEffect(player, active) {
            if (active && latestPlayIntent) player.play()
            onStopOrDispose { player.pause() }
        }
        LaunchedEffect(player) {
            player.state.collect { playbackSnapshot = it.playbackSnapshot() }
        }
        EntryImmersiveActiveSessionEffect(active, onPagingBlockedChange) {
            player.pause()
            playIntent = true
            playbackSnapshot = player.capturePlaybackSnapshot()
        }
        DisposableEffect(player) {
            onDispose {
                player.stop()
                player.release()
            }
        }
        val subtitles = rememberAnimeClipSubtitles(
            player = player,
            external = handle.stream.subtitles,
            savedKey = savedSubtitleKey,
            preferences = preferences,
            active = active,
        )
        val subtitlesLabel = stringResource(MR.strings.anime_playback_subtitles)
        DisposableEffect(switches, subtitles.available, subtitles.shown, subtitlesLabel) {
            switches.offer(
                if (subtitles.available) {
                    listOf(
                        EntryImmersiveMediaSwitch(
                            label = subtitlesLabel,
                            icon = MaterialSymbols.Rounded.Subtitles,
                            on = subtitles.shown,
                            onToggle = subtitles::toggle,
                        ),
                    )
                } else {
                    emptyList()
                },
            )
            onDispose { switches.offer(emptyList()) }
        }
        // Declared after the release above so it is disposed first, before the player it reads is gone.
        DisposableEffect(player, place) {
            place.track { EntryImmersivePlace.Playback(player.currentPositionMs.coerceAtLeast(0L)) }
            onDispose { place.untrack() }
        }

        AnimeImmersivePlayerSurface(
            player = player,
            platform = platform,
            active = active,
            controlsVisible = controlsVisible,
            controlsBottomInset = controlsBottomInset,
            playbackSnapshot = playbackSnapshot,
            isBuffering = isBuffering,
            hasRenderedFirstFrame = hasRenderedFirstFrame,
            keepAwake = keepAwake,
            playerErrorMessage = playerErrorMessage,
            muted = muted,
            subtitles = subtitles,
            metadata = metadata,
            onToggleControls = onToggleControls,
            onPlaybackSnapshotChange = { playbackSnapshot = it },
            onPlayIntentChange = { playIntent = it },
            onMutedChange = {
                muted = it
                preferences.immersiveFeedMuted = it
            },
            onClearError = { playerErrorMessage = null },
            onMediaWidthChange = onMediaWidthChange,
            modifier = modifier,
        )
    }
}

/**
 * A clip's volume for the player's own [percent] where it has one, up to as recorded; the rest of a level above that is
 * the boost. Without one, the device's volume rules, and the clip plays as recorded under it.
 */
private fun clipVolume(percent: Int?): Float {
    percent ?: return 1f
    return percent.coerceIn(0, VideoPlayer.NO_VOLUME_BOOST_PERCENT) / VideoPlayer.NO_VOLUME_BOOST_PERCENT.toFloat()
}
