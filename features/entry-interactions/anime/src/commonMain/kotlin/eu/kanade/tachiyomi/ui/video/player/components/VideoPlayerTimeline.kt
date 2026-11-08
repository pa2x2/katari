package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import eu.kanade.tachiyomi.ui.video.player.introtimes.markSource
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapter
import eu.kanade.tachiyomi.ui.video.player.playback.coerceToPlaybackDuration
import eu.kanade.tachiyomi.ui.video.player.playback.formatPlaybackTimestamp
import mihon.entry.interactions.anime.introtimes.OnlineMarker
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.MediaMarkedSpan
import mihon.presentation.kit.control.MediaTimeline
import mihon.presentation.kit.surface.MediaScrim
import mihon.presentation.kit.surface.MediaScrimEdge
import mihon.presentation.kit.surface.MediaScrimWeight
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToLong

@Composable
internal fun VideoPlayerTimeline(
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    isScrubbing: Boolean,
    seekPreviewState: VideoPlayerSeekPreviewState?,
    seekPreviewPicture: (@Composable (Modifier) -> Unit)?,
    seekPreviewAspectRatio: Float?,
    onScrubStarted: () -> Unit,
    onScrubPositionChange: (Long) -> Unit,
    onScrubFinished: () -> Unit,
    showsRemainingTime: Boolean,
    onToggleRemainingTime: () -> Unit,
    markerBoundariesMs: List<Long> = emptyList(),
    onlineMarkers: List<OnlineMarker> = emptyList(),
    chapter: VideoPlayerChapter?,
    onOpenChapters: () -> Unit,
    /** Where a mouse hovers over the bar, null once it leaves; null where nothing hovers. */
    onHover: ((Long?) -> Unit)? = null,
    /** A position a mouse hovers at, previewed as scrubbing is, named with its [hoverChapter]. */
    hoverPositionMs: Long? = null,
    hoverChapter: VideoPlayerChapter? = null,
    timeLabelTrailing: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val effectiveDurationMs = durationMs.coerceAtLeast(0L)
    val clampedPositionMs = positionMs.coerceToPlaybackDuration(effectiveDurationMs)
    val clampedBufferedPositionMs = bufferedPositionMs.coerceToPlaybackDuration(effectiveDurationMs)
    val playedFraction = clampedPositionMs.fractionOf(effectiveDurationMs)
    val durationLabel = if (effectiveDurationMs > 0L) formatPlaybackTimestamp(effectiveDurationMs) else "--:--"
    val timeLabel = if (showsRemainingTime && effectiveDurationMs > 0L) {
        "\u2212${formatPlaybackTimestamp(effectiveDurationMs - clampedPositionMs)}"
    } else {
        "${formatPlaybackTimestamp(clampedPositionMs)} / $durationLabel"
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
            val hoverMs = hoverPositionMs?.takeIf { !isScrubbing && effectiveDurationMs > 0L }
            if (isScrubbing && seekPreviewState?.visible == true) {
                VideoPlayerSeekPreview(
                    positionMs = clampedPositionMs,
                    previewFraction = playedFraction,
                    previewState = seekPreviewState,
                    previewPicture = seekPreviewPicture,
                    previewAspectRatio = seekPreviewAspectRatio,
                )
            } else if (hoverMs != null) {
                VideoPlayerSeekPreview(
                    positionMs = hoverMs,
                    previewFraction = hoverMs.fractionOf(effectiveDurationMs),
                    previewState = seekPreviewState,
                    previewPicture = seekPreviewPicture,
                    previewAspectRatio = seekPreviewAspectRatio,
                    chapterName = hoverChapter?.name?.text(),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (isScrubbing) spacing.s else KatariTheme.sizes.none),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .videoPlayerControl()
                            .clickable(
                                onClickLabel = stringResource(
                                    if (showsRemainingTime) {
                                        MR.strings.anime_playback_show_elapsed_time
                                    } else {
                                        MR.strings.anime_playback_show_remaining_time
                                    },
                                ),
                                role = Role.Button,
                                onClick = onToggleRemainingTime,
                            )
                            // Vertical room makes the label easier to hit without moving the timeline below it.
                            .padding(vertical = spacing.s),
                    ) {
                        Text(text = timeLabel, role = TextRole.Body, tone = ContentTone.OnMedia, maxLines = 1)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (chapter != null) VideoPlayerChapterLabel(chapter = chapter, onClick = onOpenChapters)
                    }
                    timeLabelTrailing?.invoke()
                }

                MediaTimeline(
                    played = playedFraction,
                    buffered = clampedBufferedPositionMs.fractionOf(effectiveDurationMs),
                    scrubbing = isScrubbing,
                    onScrubStart = onScrubStarted,
                    onScrub = { onScrubPositionChange(playbackPositionAt(it, effectiveDurationMs)) },
                    onScrubEnd = onScrubFinished,
                    positionDescription = stringResource(
                        MR.strings.anime_playback_position_of_duration,
                        formatPlaybackTimestamp(clampedPositionMs),
                        durationLabel,
                    ),
                    enabled = effectiveDurationMs > 0L,
                    segmentBoundaries = markerBoundariesMs.map { it.fractionOf(effectiveDurationMs) },
                    markedSpans = onlineMarkers.map { online ->
                        MediaMarkedSpan(
                            start = online.marker.startMs.fractionOf(effectiveDurationMs),
                            end = online.marker.endMs.fractionOf(effectiveDurationMs),
                            source = online.service.markSource,
                        )
                    },
                    onHover = onHover?.let { hover ->
                        { fraction -> hover(fraction?.let { playbackPositionAt(it, effectiveDurationMs) }) }
                    },
                    modifier = Modifier.videoPlayerControl(),
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
            positionDescription = stringResource(
                MR.strings.anime_playback_position_of_duration,
                formatPlaybackTimestamp(displayedPositionMs),
                formatPlaybackTimestamp(effectiveDurationMs),
            ),
            enabled = effectiveDurationMs > 0L,
            compact = true,
        )
    }
}

private fun Long.fractionOf(durationMs: Long): Float = if (durationMs > 0L) toFloat() / durationMs else 0f

private fun playbackPositionAt(fraction: Float, durationMs: Long): Long =
    (durationMs * fraction).roundToLong().coerceToPlaybackDuration(durationMs)
