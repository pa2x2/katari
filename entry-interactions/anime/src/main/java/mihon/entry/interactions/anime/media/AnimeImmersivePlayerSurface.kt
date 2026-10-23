package mihon.entry.interactions.anime.media

import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerPlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.capturePlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerCompactTimeline
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSeekFeedback
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekDirection
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekFeedbackState
import eu.kanade.tachiyomi.ui.video.player.gesture.nextVideoPlayerSeekFeedbackState
import eu.kanade.tachiyomi.ui.video.player.gesture.resolveVideoPlayerSeekDirectionFromTap
import eu.kanade.tachiyomi.ui.video.player.gesture.resolveVideoPlayerSeekPosition
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mihon.entry.viewer.chrome.ui.ReaderMediaLoadOverlay
import mihon.entry.viewer.chrome.ui.ReaderMediaLoadState
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.VolumeOff
import mihon.icons.materialsymbols.automirroredrounded.VolumeUp
import mihon.icons.materialsymbols.roundedfilled.Pause
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import mihon.presentation.kit.button.ContentButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.OverlayPill
import mihon.presentation.kit.surface.OverlayStyle
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.fillArgb
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
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
                stepSeconds = DOUBLE_TAP_SEEK_SECONDS,
            ),
        )
        onPlaybackSnapshotChange(player.capturePlaybackSnapshot())
        if (latestControlsVisible) latestOnToggleControls()
        seekFeedbackSequence += 1L
        seekFeedbackState = nextVideoPlayerSeekFeedbackState(
            previousState = seekFeedbackState,
            direction = direction,
            stepSeconds = DOUBLE_TAP_SEEK_SECONDS,
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
    val spacing = KatariTheme.spacing
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

    val mediaColor = SurfaceContainer.Media.fillArgb()
    Box(modifier = modifier) {
        AndroidView(
            factory = { androidContext ->
                PlayerView(androidContext).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = player
                    setKeepContentOnPlayerReset(true)
                    setEnableComposeSurfaceSyncWorkaround(true)
                    setShutterBackgroundColor(mediaColor)
                    setBackgroundColor(mediaColor)
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
            OverlayPill(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = spacing.xxl * 2 + spacing.xl),
                style = OverlayStyle.Scrim,
            ) {
                Text(
                    text = "2x",
                    modifier = Modifier.padding(horizontal = spacing.l, vertical = spacing.s),
                    role = TextRole.SectionTitle,
                )
            }
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
                    .padding(bottom = controlsBottomInset + spacing.xs),
            )
            OverlayPill(
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
                modifier = Modifier.align(Alignment.Center),
                style = OverlayStyle.Scrim,
                enabled = !isBuffering,
            ) {
                Box(
                    modifier = Modifier
                        .padding(spacing.s)
                        .size(KatariTheme.sizes.iconLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isBuffering) {
                        LoadingIndicator(modifier = Modifier.fillMaxSize(), tone = ContentTone.OnMedia)
                    } else {
                        Icon(
                            imageVector = if (playbackSnapshot.isPlaying) {
                                MaterialSymbols.RoundedFilled.Pause
                            } else {
                                MaterialSymbols.RoundedFilled.PlayArrow
                            },
                            contentDescription = stringResource(
                                if (playbackSnapshot.isPlaying) MR.strings.action_pause else MR.strings.action_play,
                            ),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            ContentButton(
                onClick = {
                    controllerInteractionSequence += 1L
                    onMutedChange(!muted)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = spacing.m, bottom = controlsBottomInset + spacing.xxl)
                    .size(KatariTheme.sizes.touchTarget),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    OverlayPill(style = OverlayStyle.Scrim) {
                        Icon(
                            imageVector = if (muted) {
                                MaterialSymbols.AutoMirroredRounded.VolumeOff
                            } else {
                                MaterialSymbols.AutoMirroredRounded.VolumeUp
                            },
                            contentDescription = stringResource(
                                if (muted) MR.strings.action_unmute else MR.strings.action_mute,
                            ),
                            modifier = Modifier
                                .padding(spacing.s)
                                .size(KatariTheme.sizes.iconSmall),
                        )
                    }
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

// Short clips in a feed; the anime player's seek step setting is for full episodes.
private const val DOUBLE_TAP_SEEK_SECONDS = 5
private const val CONTROLS_AUTO_HIDE_DELAY_MS = 3_000L
private const val SPEED_BOOST_MULTIPLIER = 2f
private const val NORMAL_PLAYBACK_SPEED = 1f
