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
import app.katari.entry.interactions.media.EntryImmersiveProgress
import app.katari.entry.interactions.media.EntryImmersiveRenderer
import app.katari.video.engine.spi.VideoEngine
import app.katari.video.engine.spi.VideoPlayerRole
import app.katari.video.engine.spi.loading.resolvedRequest
import app.katari.video.engine.spi.player.VideoPlayerEvent
import app.katari.video.engine.spi.player.VideoPlayerState
import eu.kanade.tachiyomi.ui.video.player.AnimePlayerBasePreferences
import eu.kanade.tachiyomi.ui.video.player.VideoActivePlaybackClock
import eu.kanade.tachiyomi.ui.video.player.playback.capturePlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.playback.playbackSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

internal class AnimeImmersiveRenderer(
    private val handle: EntryImmersiveHandle.Playback,
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
        onProgress: (EntryImmersiveProgress) -> Unit,
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
        val activePlaybackClock = remember(handle.chapterId, handle.stream.resolvedRequest.url) {
            VideoActivePlaybackClock()
        }
        val player = remember(handle.chapterId, handle.stream.resolvedRequest.url, activePlaybackClock) {
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

        LaunchedEffect(active) {
            if (active) onPagingBlockedChange(false)
        }
        LaunchedEffect(player, handle.resumePositionMs) {
            if (handle.resumePositionMs > 0L) player.seekTo(handle.resumePositionMs)
            player.setPlayWhenReady(active)
            player.prepare()
        }
        LaunchedEffect(player, active, muted) {
            if (active) {
                val storedMuted = preferences.immersiveFeedMuted
                if (storedMuted != muted) {
                    muted = storedMuted
                    return@LaunchedEffect
                }
            }
            player.setVolume(if (active && !muted) 1f else 0f)
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
        LaunchedEffect(player, activePlaybackClock) {
            player.state.map { it.isPlaying }.distinctUntilChanged().collect(activePlaybackClock::setActive)
        }
        LaunchedEffect(player, handle.chapterId, active) {
            if (!active) return@LaunchedEffect
            while (isActive) {
                delay(PROGRESS_SAVE_INTERVAL_MS)
                val snapshot = player.capturePlaybackSnapshot()
                onProgress(
                    EntryImmersiveProgress.Playback(
                        positionMs = snapshot.positionMs,
                        durationMs = snapshot.durationMs,
                        activeDurationMs = activePlaybackClock.checkpoint(),
                    ),
                )
            }
        }
        EntryImmersiveActiveSessionEffect(active, onPagingBlockedChange) {
            val snapshot = player.capturePlaybackSnapshot()
            onProgress(
                EntryImmersiveProgress.Playback(
                    positionMs = snapshot.positionMs,
                    durationMs = snapshot.durationMs,
                    activeDurationMs = activePlaybackClock.checkpoint(),
                    resetSession = true,
                ),
            )
            player.pause()
            player.seekTo(0L)
            playIntent = true
            playbackSnapshot = player.capturePlaybackSnapshot()
        }
        DisposableEffect(player) {
            onDispose {
                val snapshot = player.capturePlaybackSnapshot()
                onProgress(
                    EntryImmersiveProgress.Playback(
                        positionMs = snapshot.positionMs,
                        durationMs = snapshot.durationMs,
                        activeDurationMs = activePlaybackClock.checkpoint(),
                    ),
                )
                player.stop()
                player.release()
            }
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
            onToggleControls = onToggleControls,
            onPlaybackSnapshotChange = { playbackSnapshot = it },
            onPlayIntentChange = { playIntent = it },
            onMutedChange = {
                muted = it
                preferences.immersiveFeedMuted = it
            },
            onClearError = { playerErrorMessage = null },
            modifier = modifier,
        )
    }
}

private const val PROGRESS_SAVE_INTERVAL_MS = 10_000L
