package mihon.entry.interactions.anime.media

import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerPlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSeekDirection
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSeekFeedbackState
import eu.kanade.tachiyomi.ui.video.player.capturePlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerCompactTimeline
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSeekFeedback
import eu.kanade.tachiyomi.ui.video.player.nextVideoPlayerSeekFeedbackState
import eu.kanade.tachiyomi.ui.video.player.resolveVideoPlayerSeekDirectionFromTap
import eu.kanade.tachiyomi.ui.video.player.resolveVideoPlayerSeekPosition
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderMediaLoadOverlay
import tachiyomi.presentation.core.components.reader.ReaderMediaLoadState
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun AnimeImmersivePlayerSurface(
    player: ExoPlayer,
    active: Boolean,
    controlsVisible: Boolean,
    controlsBottomInset: Dp,
    playbackSnapshot: VideoPlayerPlaybackSnapshot,
    isBuffering: Boolean,
    hasRenderedFirstFrame: Boolean,
    playerErrorMessage: String?,
    muted: Boolean,
    onToggleControls: () -> Unit,
    onPlaybackSnapshotChange: (VideoPlayerPlaybackSnapshot) -> Unit,
    onPlayIntentChange: (Boolean) -> Unit,
    onMutedChange: (Boolean) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var speedBoostActive by remember(player) { mutableStateOf(false) }
    var controllerInteractionSequence by remember(player) { mutableStateOf(0L) }
    var seekFeedbackSequence by remember(player) { mutableStateOf(0L) }
    var seekFeedbackState by remember(player) { mutableStateOf<VideoPlayerSeekFeedbackState?>(null) }
    val latestControlsVisible by rememberUpdatedState(controlsVisible)
    val latestOnToggleControls by rememberUpdatedState(onToggleControls)
    val latestSeekFeedbackState by rememberUpdatedState(seekFeedbackState)
    val performGestureSeek: (VideoPlayerSeekDirection) -> Unit = { direction ->
        val durationMs = playbackSnapshot.durationMs.takeIf { it > 0L }
            ?: player.duration.coerceAtLeast(0L)
        player.seekTo(
            resolveVideoPlayerSeekPosition(
                positionMs = player.currentPosition,
                durationMs = durationMs,
                direction = direction,
            ),
        )
        onPlaybackSnapshotChange(player.capturePlaybackSnapshot())
        if (latestControlsVisible) latestOnToggleControls()
        seekFeedbackSequence += 1L
        seekFeedbackState = nextVideoPlayerSeekFeedbackState(
            previousState = seekFeedbackState,
            direction = direction,
            hidePlayerChrome = true,
            sequence = seekFeedbackSequence,
            updatedAtMillis = System.currentTimeMillis(),
        )
    }
    val latestPerformGestureSeek by rememberUpdatedState(performGestureSeek)
    val videoAlpha by animateFloatAsState(
        targetValue = if (hasRenderedFirstFrame) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "immersiveVideoAlpha",
    )
    LaunchedEffect(
        active,
        controlsVisible,
        controllerInteractionSequence,
        playbackSnapshot.isPlaying,
        isBuffering,
    ) {
        if (!active || !controlsVisible || !playbackSnapshot.isPlaying || isBuffering) {
            return@LaunchedEffect
        }

        delay(CONTROLS_AUTO_HIDE_DELAY_MS)
        latestOnToggleControls()
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { androidContext ->
                PlayerView(androidContext).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = player
                    setKeepContentOnPlayerReset(true)
                    setEnableComposeSurfaceSyncWorkaround(true)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.setKeepContentOnPlayerReset(true)
                playerView.useController = false
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(videoAlpha),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(player) {
                    var suppressNextTap = false
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            suppressNextTap = false
                            resolveVideoPlayerSeekDirectionFromTap(
                                tapX = offset.x,
                                viewportWidth = size.width.toFloat(),
                            )?.let(latestPerformGestureSeek)
                        },
                        onTap = { offset ->
                            if (suppressNextTap) {
                                suppressNextTap = false
                            } else if (latestSeekFeedbackState != null) {
                                resolveVideoPlayerSeekDirectionFromTap(
                                    tapX = offset.x,
                                    viewportWidth = size.width.toFloat(),
                                )?.let(latestPerformGestureSeek)
                            } else {
                                latestOnToggleControls()
                            }
                        },
                        onPress = {
                            coroutineScope {
                                var boosted = false
                                val boostJob = launch {
                                    delay(SPEED_BOOST_PRESS_DELAY_MS)
                                    player.setPlaybackSpeed(SPEED_BOOST_MULTIPLIER)
                                    boosted = true
                                    speedBoostActive = true
                                }
                                var released = false
                                try {
                                    released = tryAwaitRelease()
                                } finally {
                                    boostJob.cancel()
                                    if (boosted) {
                                        suppressNextTap = released
                                        player.setPlaybackSpeed(NORMAL_PLAYBACK_SPEED)
                                        speedBoostActive = false
                                        onPlaybackSnapshotChange(player.capturePlaybackSnapshot())
                                    }
                                }
                            }
                        },
                    )
                },
        )

        VideoPlayerSeekFeedback(
            feedbackState = seekFeedbackState,
            onDismissed = { seekFeedbackState = null },
        )

        if (speedBoostActive) {
            Text(
                text = "2x",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 84.dp)
                    .background(Color.Black.copy(alpha = 0.48f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }

        if (active && controlsVisible) {
            VideoPlayerCompactTimeline(
                positionMs = playbackSnapshot.positionMs,
                durationMs = playbackSnapshot.durationMs,
                bufferedPositionMs = playbackSnapshot.bufferedPositionMs,
                onSeek = { positionMs ->
                    controllerInteractionSequence += 1L
                    player.seekTo(positionMs)
                    if (playbackSnapshot.playbackEnded) player.play()
                    onPlayIntentChange(true)
                    onPlaybackSnapshotChange(player.capturePlaybackSnapshot())
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = controlsBottomInset + 4.dp),
            )
            IconButton(
                onClick = {
                    controllerInteractionSequence += 1L
                    if (playbackSnapshot.playbackEnded) player.seekTo(0L)
                    if (player.isPlaying) {
                        player.pause()
                        onPlayIntentChange(false)
                    } else {
                        player.play()
                        onPlayIntentChange(true)
                    }
                    onPlaybackSnapshotChange(player.capturePlaybackSnapshot())
                },
                enabled = !isBuffering,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.48f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = if (playbackSnapshot.isPlaying) {
                                Icons.Filled.Pause
                            } else {
                                Icons.Filled.PlayArrow
                            },
                            contentDescription = stringResource(
                                if (playbackSnapshot.isPlaying) {
                                    MR.strings.action_pause
                                } else {
                                    MR.strings.action_play
                                },
                            ),
                            modifier = Modifier.size(28.dp),
                            tint = Color.White,
                        )
                    }
                }
            }
            IconButton(
                onClick = {
                    controllerInteractionSequence += 1L
                    onMutedChange(!muted)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 12.dp,
                        bottom = controlsBottomInset + 36.dp,
                    )
                    .size(48.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.48f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (muted) {
                            Icons.AutoMirrored.Outlined.VolumeOff
                        } else {
                            Icons.AutoMirrored.Outlined.VolumeUp
                        },
                        contentDescription = stringResource(
                            if (muted) MR.strings.action_unmute else MR.strings.action_mute,
                        ),
                        modifier = Modifier.size(20.dp),
                        tint = Color.White,
                    )
                }
            }
        }

        ReaderMediaLoadOverlay(
            state = when {
                playerErrorMessage != null -> ReaderMediaLoadState.Failed(playerErrorMessage)
                !hasRenderedFirstFrame || (isBuffering && !controlsVisible) -> ReaderMediaLoadState.Loading()
                else -> ReaderMediaLoadState.Ready
            },
            showBackground = !hasRenderedFirstFrame,
            onBackgroundClick = latestOnToggleControls,
            onRetry = {
                onClearError()
                onPlayIntentChange(true)
                player.prepare()
                player.play()
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val SPEED_BOOST_PRESS_DELAY_MS = 350L
private const val CONTROLS_AUTO_HIDE_DELAY_MS = 3_000L
private const val SPEED_BOOST_MULTIPLIER = 2f
private const val NORMAL_PLAYBACK_SPEED = 1f
