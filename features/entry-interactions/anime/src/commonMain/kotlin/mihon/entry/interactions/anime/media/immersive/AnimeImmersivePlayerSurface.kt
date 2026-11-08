package mihon.entry.interactions.anime.media.immersive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerCompactTimeline
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSeekFeedback
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekDirection
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekFeedbackState
import eu.kanade.tachiyomi.ui.video.player.gesture.nextVideoPlayerSeekFeedbackState
import eu.kanade.tachiyomi.ui.video.player.gesture.resolveVideoPlayerSeekPosition
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.playback.capturePlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitleAppearance
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitles
import eu.kanade.tachiyomi.ui.video.player.subtitle.videoSubtitleLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mihon.entry.viewer.chrome.ui.loading.ReaderMediaLoadOverlay
import mihon.entry.viewer.chrome.ui.loading.ReaderMediaLoadState
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
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.video.engine.spi.player.VideoPlayer
import tachiyomi.domain.entry.model.PictureFraming
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs

@Composable
internal fun AnimeImmersivePlayerSurface(
    player: VideoPlayer,
    platform: AnimeClipPlatform,
    active: Boolean,
    controlsVisible: Boolean,
    controlsBottomInset: Dp,
    playbackSnapshot: VideoPlayerPlaybackSnapshot,
    isBuffering: Boolean,
    hasRenderedFirstFrame: Boolean,
    keepAwake: Boolean,
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
    val performSeek: (VideoPlayerSeekDirection, Int) -> Unit = { direction, stepSeconds ->
        val durationMs = playbackSnapshot.durationMs.takeIf { it > 0L }
            ?: player.state.value.durationMs?.coerceAtLeast(0L)
            ?: 0L
        val basePositionMs = player.currentPositionMs
        val targetPositionMs = resolveVideoPlayerSeekPosition(
            positionMs = basePositionMs,
            durationMs = durationMs,
            direction = direction,
            stepSeconds = stepSeconds,
        )
        player.seekTo(targetPositionMs)
        onPlaybackSnapshotChange(player.capturePlaybackSnapshot())
        if (latestControlsVisible) latestOnToggleControls()
        seekFeedbackSequence += 1L
        seekFeedbackState = nextVideoPlayerSeekFeedbackState(
            previousState = seekFeedbackState,
            direction = direction,
            movedMs = abs(targetPositionMs - basePositionMs),
            hidePlayerChrome = true,
            sequence = seekFeedbackSequence,
            updatedAtMillis = System.currentTimeMillis(),
        )
    }
    val togglePlayback = {
        controllerInteractionSequence += 1L
        if (playbackSnapshot.playbackEnded) player.seekTo(0L)
        if (player.state.value.isPlaying) {
            player.pause()
            onPlayIntentChange(false)
        } else {
            player.play()
            onPlayIntentChange(true)
        }
        onPlaybackSnapshotChange(player.capturePlaybackSnapshot())
    }
    val toggleMute = {
        controllerInteractionSequence += 1L
        onMutedChange(!muted)
    }
    val latestActive by rememberUpdatedState(active)
    val latestPerformSeek by rememberUpdatedState(performSeek)
    val latestTogglePlayback by rememberUpdatedState(togglePlayback)
    val latestToggleMute by rememberUpdatedState(toggleMute)
    val latestOnPlaybackSnapshotChange by rememberUpdatedState(onPlaybackSnapshotChange)
    val inputTarget = remember(player) {
        object : AnimeClipInputTarget {
            override val active get() = latestActive
            override val seekFeedbackShown get() = latestSeekFeedbackState != null

            override fun toggleControls() = latestOnToggleControls()

            override fun togglePlayback() = latestTogglePlayback()

            override fun toggleMute() = latestToggleMute()

            override fun seek(direction: VideoPlayerSeekDirection, stepSeconds: Int) =
                latestPerformSeek(direction, stepSeconds)

            override fun setSpeedBoost(boosted: Boolean) {
                player.setSpeed(if (boosted) SPEED_BOOST_MULTIPLIER else NORMAL_PLAYBACK_SPEED)
                speedBoostActive = boosted
                if (!boosted) latestOnPlaybackSnapshotChange(player.capturePlaybackSnapshot())
            }
        }
    }
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

    val playerState = remember(player) {
        player.state.map { it.videoSize?.displayAspectRatio to it.cues }.distinctUntilChanged()
    }.collectAsState(initial = player.state.value.let { it.videoSize?.displayAspectRatio to it.cues })
    val (aspectRatio, cues) = playerState.value
    val subtitleGapPx = with(LocalDensity.current) { spacing.s.toPx() }
    platform.KeepAwake(playing = keepAwake)
    Box(modifier = modifier) {
        platform.Picture(
            player = player,
            aspectRatio = aspectRatio,
            target = inputTarget,
            modifier = Modifier
                .fillMaxSize()
                .alpha(videoAlpha),
        )
        VideoSubtitles(
            cues = cues,
            appearance = DEFAULT_APPEARANCE,
            layout = { width, height ->
                videoSubtitleLayout(
                    playerWidth = width,
                    playerHeight = height,
                    aspectRatio = aspectRatio,
                    framing = PictureFraming.Fit,
                    appearance = DEFAULT_APPEARANCE,
                    keepInPicture = true,
                    gapPx = subtitleGapPx,
                )
            },
            modifier = Modifier.fillMaxSize(),
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
                onClick = togglePlayback,
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
                onClick = toggleMute,
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

// A clip has no subtitle settings, so a subtitle its stream shows by default looks as the player's do by default
private val DEFAULT_APPEARANCE = VideoSubtitleAppearance()

private const val CONTROLS_AUTO_HIDE_DELAY_MS = 3_000L
private const val SPEED_BOOST_MULTIPLIER = 2f
private const val NORMAL_PLAYBACK_SPEED = 1f
