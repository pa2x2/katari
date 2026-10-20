package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.media3.exoplayer.ExoPlayer
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSeekPreviewState
import eu.kanade.tachiyomi.ui.video.player.coerceToPlaybackDuration
import eu.kanade.tachiyomi.ui.video.player.formatPlaybackTimestamp
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.MediaTimeline
import mihon.presentation.kit.surface.MediaScrim
import mihon.presentation.kit.surface.MediaScrimEdge
import mihon.presentation.kit.surface.MediaScrimWeight
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import kotlin.math.roundToLong

@Composable
internal fun VideoPlayerTimeline(
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    isScrubbing: Boolean,
    seekPreviewState: VideoPlayerSeekPreviewState?,
    seekPreviewPlayer: ExoPlayer?,
    seekPreviewAspectRatio: Float?,
    onScrubStarted: () -> Unit,
    onScrubPositionChange: (Long) -> Unit,
    onScrubFinished: () -> Unit,
    footerContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val effectiveDurationMs = durationMs.coerceAtLeast(0L)
    val clampedPositionMs = positionMs.coerceToPlaybackDuration(effectiveDurationMs)
    val clampedBufferedPositionMs = bufferedPositionMs.coerceToPlaybackDuration(effectiveDurationMs)
    val playedFraction = clampedPositionMs.fractionOf(effectiveDurationMs)
    val timeLabel = buildString {
        append(formatPlaybackTimestamp(clampedPositionMs))
        append(" / ")
        append(if (effectiveDurationMs > 0L) formatPlaybackTimestamp(effectiveDurationMs) else "--:--")
    }
    val spacing = KatariTheme.spacing

    MediaScrim(
        edge = MediaScrimEdge.Bottom,
        modifier = modifier.fillMaxWidth(),
        weight = MediaScrimWeight.Controls,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .videoPlayerSafeContentPadding(includeBottom = true)
                .padding(horizontal = spacing.l)
                .padding(top = spacing.s, bottom = spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isScrubbing && seekPreviewState?.visible == true) {
                VideoPlayerSeekPreview(
                    positionMs = clampedPositionMs,
                    previewFraction = playedFraction,
                    previewState = seekPreviewState,
                    previewPlayer = seekPreviewPlayer,
                    previewAspectRatio = seekPreviewAspectRatio,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (isScrubbing) spacing.s else KatariTheme.sizes.none),
            ) {
                Box(modifier = Modifier.padding(bottom = spacing.s)) {
                    Text(text = timeLabel, role = TextRole.Body, tone = ContentTone.OnMedia, maxLines = 1)
                }

                MediaTimeline(
                    played = playedFraction,
                    buffered = clampedBufferedPositionMs.fractionOf(effectiveDurationMs),
                    scrubbing = isScrubbing,
                    onScrubStart = onScrubStarted,
                    onScrub = { onScrubPositionChange(playbackPositionAt(it, effectiveDurationMs)) },
                    onScrubEnd = onScrubFinished,
                    enabled = effectiveDurationMs > 0L,
                )

                if (footerContent != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = spacing.xs),
                    ) {
                        footerContent()
                    }
                }
            }
        }
    }
}

@Composable
internal fun VideoPlayerCompactTimeline(
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val effectiveDurationMs = durationMs.coerceAtLeast(0L)
    var isScrubbing by remember(effectiveDurationMs) { mutableStateOf(false) }
    var scrubPositionMs by remember(effectiveDurationMs) { mutableStateOf(positionMs) }
    val displayedPositionMs = if (isScrubbing) scrubPositionMs else positionMs

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isScrubbing) {
            Pill(
                text = "${formatPlaybackTimestamp(scrubPositionMs)} / " + formatPlaybackTimestamp(effectiveDurationMs),
                container = SurfaceContainer.MediaScrim,
            )
        }
        MediaTimeline(
            played = displayedPositionMs.fractionOf(effectiveDurationMs),
            buffered = bufferedPositionMs.fractionOf(effectiveDurationMs),
            scrubbing = isScrubbing,
            onScrubStart = { isScrubbing = true },
            onScrub = { scrubPositionMs = playbackPositionAt(it, effectiveDurationMs) },
            onScrubEnd = {
                onSeek(scrubPositionMs)
                isScrubbing = false
            },
            enabled = effectiveDurationMs > 0L,
            compact = true,
        )
    }
}

private fun Long.fractionOf(durationMs: Long): Float = if (durationMs > 0L) toFloat() / durationMs else 0f

private fun playbackPositionAt(fraction: Float, durationMs: Long): Long =
    (durationMs * fraction).roundToLong().coerceToPlaybackDuration(durationMs)
