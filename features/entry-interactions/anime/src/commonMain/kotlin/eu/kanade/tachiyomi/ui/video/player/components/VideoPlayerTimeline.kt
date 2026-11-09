package eu.kanade.tachiyomi.ui.video.player.components

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
import app.katari.entry.interactions.anime.introtimes.OnlineMarker
import app.katari.entry.viewer.chrome.ui.pointer.mediaControl
import app.katari.presentation.kit.control.MediaMarkedSpan
import app.katari.presentation.kit.control.MediaTimeline
import app.katari.presentation.kit.surface.MediaScrim
import app.katari.presentation.kit.surface.MediaScrimEdge
import app.katari.presentation.kit.surface.MediaScrimWeight
import app.katari.presentation.kit.surface.Pill
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.ui.video.player.introtimes.markSource
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapter
import eu.kanade.tachiyomi.ui.video.player.playback.coerceToPlaybackDuration
import eu.kanade.tachiyomi.ui.video.player.playback.formatPlaybackTimestamp
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToLong

/**
 * The controls along the bottom of the player on their shade: the timeline with its marked parts, the frame at the
 * scrubbed or hovered position floating above it, and [footerContent], the toolbar under it.
 */
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
    markerBoundariesMs: List<Long> = emptyList(),
    onlineMarkers: List<OnlineMarker> = emptyList(),
    /** Where a mouse hovers over the bar, null once it leaves; null where nothing hovers. */
    onHover: ((Long?) -> Unit)? = null,
    /** A position a mouse hovers at, previewed as scrubbing is, named with its [hoverChapter]. */
    hoverPositionMs: Long? = null,
    hoverChapter: VideoPlayerChapter? = null,
    footerContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val effectiveDurationMs = durationMs.coerceAtLeast(0L)
    val clampedPositionMs = positionMs.coerceToPlaybackDuration(effectiveDurationMs)
    val clampedBufferedPositionMs = bufferedPositionMs.coerceToPlaybackDuration(effectiveDurationMs)
    val playedFraction = clampedPositionMs.fractionOf(effectiveDurationMs)
    val durationLabel = if (effectiveDurationMs > 0L) formatPlaybackTimestamp(effectiveDurationMs) else "--:--"
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
            // The frame at the scrubbed or hovered position floats above the bar, over the picture
            val previewMs = when {
                isScrubbing && seekPreviewState?.visible == true -> clampedPositionMs
                else -> hoverMs
            }
            val hoverChapterName = hoverChapter?.name?.text()

            Column(modifier = Modifier.fillMaxWidth()) {
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
                    previewAt = previewMs?.fractionOf(effectiveDurationMs),
                    preview = previewMs?.let { positionMs ->
                        {
                            VideoPlayerSeekPreview(
                                positionMs = positionMs,
                                previewState = seekPreviewState,
                                previewPicture = seekPreviewPicture,
                                previewAspectRatio = seekPreviewAspectRatio,
                                chapterName = hoverChapterName.takeIf { !isScrubbing },
                            )
                        }
                    },
                    modifier = Modifier.mediaControl(),
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

/**
 * The slim timeline of a clip in the immersive feed: the time played and the length above it, and the time under a
 * hovering mouse floating over the bar. Scrubbing moves only the shown time; the clip seeks when it is let go.
 */
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
    var hoverMs by remember(effectiveDurationMs) { mutableStateOf<Long?>(null) }
    val durationLabel = if (effectiveDurationMs > 0L) formatPlaybackTimestamp(effectiveDurationMs) else "--:--"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.m),
    ) {
        Pill(
            text = "${formatPlaybackTimestamp(displayedPositionMs)} / $durationLabel",
            container = SurfaceContainer.MediaScrim,
        )
        val shownHoverMs = hoverMs?.takeIf { !isScrubbing && effectiveDurationMs > 0L }
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
                durationLabel,
            ),
            enabled = effectiveDurationMs > 0L,
            compact = true,
            onHover = { fraction -> hoverMs = fraction?.let { playbackPositionAt(it, effectiveDurationMs) } },
            previewAt = shownHoverMs?.fractionOf(effectiveDurationMs),
            preview = shownHoverMs?.let { at ->
                { Pill(text = formatPlaybackTimestamp(at), container = SurfaceContainer.MediaScrim) }
            },
        )
    }
}

private fun Long.fractionOf(durationMs: Long): Float = if (durationMs > 0L) toFloat() / durationMs else 0f

private fun playbackPositionAt(fraction: Float, durationMs: Long): Long =
    (durationMs * fraction).roundToLong().coerceToPlaybackDuration(durationMs)
