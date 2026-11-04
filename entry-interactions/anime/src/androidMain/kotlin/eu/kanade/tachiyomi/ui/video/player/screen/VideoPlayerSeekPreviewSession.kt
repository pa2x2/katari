package eu.kanade.tachiyomi.ui.video.player.screen

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSeekPreviewState
import eu.kanade.tachiyomi.ui.video.player.playback.coerceToPlaybackDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mihon.video.engine.spi.VideoPlayerRole
import mihon.video.engine.spi.player.VideoPlayer
import mihon.video.engine.spi.player.VideoPlayerEvent
import mihon.video.engine.spi.player.VideoPlayerState
import kotlin.math.abs

private const val SEEK_PREVIEW_FIRST_DEBOUNCE_MS = 50L
private const val SEEK_PREVIEW_THROTTLE_MS = 200L
private const val SEEK_PREVIEW_MIN_DELTA_MS = 2_000L

/**
 * The live frame above the timeline while scrubbing. A second, silent player on the same stream seeks to the scrub
 * position, at most every 200 ms and only for moves of 2 seconds or more; once it fails it stays off until the next
 * playback. [player] is null while the seek preview setting is off.
 */
@Stable
internal class VideoPlayerSeekPreviewSession(
    val player: VideoPlayer?,
    private val enabled: Boolean,
    state: MutableState<VideoPlayerSeekPreviewState>,
    aspectRatio: MutableState<Float?>,
    failed: MutableState<Boolean>,
    lastRequestAtMs: MutableState<Long>,
    lastPositionMs: MutableState<Long?>,
) {
    var state by state
        private set
    val aspectRatio by aspectRatio
    val failed by failed
    private var lastRequestAtMs by lastRequestAtMs
    private var lastPositionMs by lastPositionMs

    fun onScrubStarted(positionMs: Long, durationMs: Long) {
        state = if (
            enabled &&
            !failed &&
            durationMs > 0L
        ) {
            VideoPlayerSeekPreviewState(
                positionMs = positionMs,
                visible = true,
                loading = true,
            )
        } else {
            VideoPlayerSeekPreviewState()
        }
    }

    fun onScrubPositionChange(targetPositionMs: Long, durationMs: Long) {
        val lastPreviewPosition = lastPositionMs
        if (
            enabled &&
            !failed &&
            durationMs > 0L &&
            (
                lastPreviewPosition == null ||
                    abs(targetPositionMs - lastPreviewPosition) >= SEEK_PREVIEW_MIN_DELTA_MS
                )
        ) {
            state = state.copy(
                positionMs = targetPositionMs,
                visible = true,
                loading = true,
            )
        }
    }

    fun onScrubFinished() {
        state = VideoPlayerSeekPreviewState()
    }

    /**
     * Asks the preview player for the frame at the scrub position while the viewer scrubs, and hides the preview
     * whenever it can't show one.
     */
    @Composable
    fun FrameRequestsEffect(
        ui: VideoPlayerWatchUiState,
        visit: VideoPlayerVisitState,
        isInPictureInPictureMode: Boolean,
        isSourceSwitching: Boolean,
    ) {
        LaunchedEffect(
            enabled,
            player,
            ui.isScrubbing,
            ui.scrubPositionMs,
            ui.playbackSnapshot.durationMs,
            isInPictureInPictureMode,
            visit.controlsLocked,
            ui.startupOverlayVisible,
            isSourceSwitching,
            failed,
        ) {
            val previewPlayer = player
            val durationMs = ui.playbackSnapshot.durationMs
            if (
                !enabled ||
                previewPlayer == null ||
                !ui.isScrubbing ||
                durationMs <= 0L ||
                isInPictureInPictureMode ||
                visit.controlsLocked ||
                ui.startupOverlayVisible ||
                isSourceSwitching ||
                failed
            ) {
                state = VideoPlayerSeekPreviewState()
                return@LaunchedEffect
            }

            val targetPositionMs = ui.scrubPositionMs.coerceToPlaybackDuration(durationMs)
            val lastPreviewPosition = lastPositionMs
            if (
                lastPreviewPosition != null &&
                abs(targetPositionMs - lastPreviewPosition) < SEEK_PREVIEW_MIN_DELTA_MS
            ) {
                return@LaunchedEffect
            }

            val now = SystemClock.elapsedRealtime()
            val delayMs = if (lastRequestAtMs == 0L) {
                SEEK_PREVIEW_FIRST_DEBOUNCE_MS
            } else {
                (SEEK_PREVIEW_THROTTLE_MS - (now - lastRequestAtMs)).coerceAtLeast(0L)
            }
            delay(delayMs)

            val latestTargetPositionMs = ui.scrubPositionMs.coerceToPlaybackDuration(durationMs)
            val latestLastPreviewPosition = lastPositionMs
            if (
                latestLastPreviewPosition != null &&
                abs(latestTargetPositionMs - latestLastPreviewPosition) < SEEK_PREVIEW_MIN_DELTA_MS
            ) {
                return@LaunchedEffect
            }

            state = VideoPlayerSeekPreviewState(
                positionMs = latestTargetPositionMs,
                visible = true,
                loading = true,
                available = state.available,
            )
            lastRequestAtMs = SystemClock.elapsedRealtime()
            lastPositionMs = latestTargetPositionMs
            previewPlayer.seekTo(latestTargetPositionMs)
            if (previewPlayer.state.value.playback == VideoPlayerState.Playback.Idle) {
                previewPlayer.prepare()
            }
        }
    }
}

/**
 * The seek preview for [current]. Its preview player is made anew for each episode, stream, playback revision and
 * change of [enabled], and released with it.
 */
@Composable
internal fun rememberVideoPlayerSeekPreviewSession(
    current: VideoPlayerViewModel.State.Ready,
    subtitlePayloadKey: String,
    enabled: Boolean,
    host: VideoPlayerScreenHost,
): VideoPlayerSeekPreviewSession {
    val state = remember(current.chapterId, current.streamUrl, subtitlePayloadKey) {
        mutableStateOf(VideoPlayerSeekPreviewState())
    }
    val aspectRatio = remember(current.chapterId, current.streamUrl, current.playbackRevision, enabled) {
        mutableStateOf<Float?>(null)
    }
    val failed = remember(current.chapterId, current.streamUrl, subtitlePayloadKey) { mutableStateOf(false) }
    val lastRequestAtMs = remember(current.chapterId, current.streamUrl, subtitlePayloadKey) { mutableStateOf(0L) }
    val lastPositionMs = remember(current.chapterId, current.streamUrl, subtitlePayloadKey) {
        mutableStateOf<Long?>(null)
    }
    val player = remember(current.chapterId, current.streamUrl, current.playbackRevision, enabled) {
        if (!enabled) {
            null
        } else {
            host.engine.createPlayer(
                stream = current.stream.copy(subtitles = emptyList(), audioTracks = emptyList()),
                role = VideoPlayerRole.SeekPreview,
            ).also { player ->
                player.addListener { event ->
                    if (event is VideoPlayerEvent.Failed) {
                        failed.value = true
                        state.value = VideoPlayerSeekPreviewState()
                    }
                }
            }
        }
    }
    LaunchedEffect(player) {
        player ?: return@LaunchedEffect
        // ExoPlayer leaves Ready as soon as it is asked to seek, so any Ready after that has the frame at the requested
        // position, even when buffering came and went between two looks at the state.
        player.state.collect { playerState ->
            if (playerState.playback == VideoPlayerState.Playback.Ready) {
                state.value = state.value.copy(loading = false, available = true)
            }
        }
    }
    LaunchedEffect(player) {
        player ?: return@LaunchedEffect
        player.state.map { it.videoSize }.distinctUntilChanged().collect { videoSize ->
            aspectRatio.value = videoSize?.displayAspectRatio
        }
    }
    DisposableEffect(player) {
        onDispose {
            player?.release()
        }
    }
    return remember(player, enabled, state, aspectRatio, failed, lastRequestAtMs, lastPositionMs) {
        VideoPlayerSeekPreviewSession(
            player = player,
            enabled = enabled,
            state = state,
            aspectRatio = aspectRatio,
            failed = failed,
            lastRequestAtMs = lastRequestAtMs,
            lastPositionMs = lastPositionMs,
        )
    }
}
