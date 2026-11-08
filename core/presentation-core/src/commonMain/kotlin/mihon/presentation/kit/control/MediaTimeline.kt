package mihon.presentation.kit.control

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

/**
 * A playback position bar laid over video: the played part in the primary colour with a thumb, then the buffered
 * part and the rest of the track in the media colours. [played] and [buffered] are fractions of the whole, 0..1.
 * [segmentBoundaries] are fractions where the video's parts meet, such as the end of an intro; the track breaks there.
 * [markedSpans] take their source's colour, played or not, so parts from different sources, such as intro times found
 * by two services, tell apart; [MediaMarkSwatch] names a colour beside its source.
 *
 * Pressing the bar starts a scrub: [onScrubStart], then [onScrub] with the touched fraction as the finger moves, then
 * [onScrubEnd] when it lifts or the gesture is cancelled. While [scrubbing] the track and thumb grow. A disabled bar,
 * such as before the duration is known, swallows touches without scrubbing. [compact] is the slim bar of a small
 * player, whose thumb only shows while scrubbing. [onHover] gets the fraction under a hovering mouse, null once it
 * leaves the bar or presses.
 *
 * [preview], such as a frame of the video at the hovered or scrubbed position, floats above the bar centred over the
 * fraction [previewAt], kept within the bar's width and no wider than a thumbnail. It takes no room, so the controls
 * around the bar and the shade behind them keep their size while it comes and goes.
 *
 * Accessibility services see a slider: [positionDescription] is read as its value, such as "0:29 of 23:46", and
 * setting the value runs one whole scrub to that fraction.
 */
@Composable
fun MediaTimeline(
    played: Float,
    buffered: Float,
    scrubbing: Boolean,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier,
    positionDescription: String? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
    segmentBoundaries: List<Float> = emptyList(),
    markedSpans: List<MediaMarkedSpan> = emptyList(),
    onHover: ((Float?) -> Unit)? = null,
    previewAt: Float? = null,
    preview: (@Composable () -> Unit)? = null,
) {
    val geometry = if (compact) CompactGeometry else FullGeometry
    val animatedTrack by animateDpAsState(geometry.track(scrubbing), tween(SIZE_ANIMATION_MS), label = "track")
    val animatedThumb by animateDpAsState(geometry.thumb(scrubbing), tween(SIZE_ANIMATION_MS), label = "thumb")
    val animatedHalo by animateDpAsState(geometry.halo(scrubbing), tween(HALO_ANIMATION_MS), label = "halo")
    // The compact bar has always switched sizes at once; only the full bar eases between them.
    val track = if (compact) geometry.track(scrubbing) else animatedTrack
    val thumb = if (compact) geometry.thumb(scrubbing) else animatedThumb
    val halo = if (compact) geometry.halo(scrubbing) else animatedHalo
    val thumbExtent = maxOf(thumb, halo)
    // The full bar's height is its touch target, so it stays put and stays easy to grab whatever size the thumb is.
    val height = if (compact) CompactHeight else KatariTheme.sizes.touchTarget

    val playedFraction = played.coerceIn(0f, 1f)
    val bufferedFraction = buffered.coerceIn(playedFraction, 1f)
    val colors = KatariTheme.colors
    val restColor = colors.onMedia.copy(alpha = REST_TRACK_ALPHA)
    val bufferedColor = colors.onMedia.copy(alpha = BUFFERED_TRACK_ALPHA)
    val playedColor = colors.primary
    val haloColor = colors.primary.copy(alpha = HALO_ALPHA)
    val markColors = colors.mediaMarks

    val latestEnabled by rememberUpdatedState(enabled)
    val latestThumbExtent by rememberUpdatedState(thumbExtent)
    val latestOnScrubStart by rememberUpdatedState(onScrubStart)
    val latestOnScrub by rememberUpdatedState(onScrub)
    val latestOnScrubEnd by rememberUpdatedState(onScrubEnd)
    val latestOnHover by rememberUpdatedState(onHover)
    // A bar that goes away under the mouse, as with the controls hiding, never sees it leave
    DisposableEffect(Unit) { onDispose { latestOnHover?.invoke(null) } }

    val label = stringResource(MR.strings.media_timeline_label)
    val previewGap = KatariTheme.spacing.s

    Layout(
        content = {
            Canvas(
                modifier = modifier
                    .fillMaxWidth()
                    .height(height)
                    .semantics {
                        contentDescription = label
                        progressBarRangeInfo = ProgressBarRangeInfo(playedFraction, 0f..1f)
                        positionDescription?.let { stateDescription = it }
                        if (enabled) {
                            setProgress { target ->
                                latestOnScrubStart()
                                latestOnScrub(target.coerceIn(0f, 1f))
                                latestOnScrubEnd()
                                true
                            }
                        } else {
                            disabled()
                        }
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val onHover = latestOnHover ?: continue
                                val change = event.changes.firstOrNull() ?: continue
                                val hovering = change.type == PointerType.Mouse &&
                                    event.type != PointerEventType.Exit &&
                                    !change.pressed &&
                                    latestEnabled
                                if (hovering) {
                                    val inset = latestThumbExtent.toPx() / 2f
                                    val trackWidth = (size.width - inset * 2f).coerceAtLeast(1f)
                                    onHover(((change.position.x - inset) / trackWidth).coerceIn(0f, 1f))
                                } else {
                                    onHover(null)
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (!latestEnabled) {
                                down.consume()
                                return@awaitEachGesture
                            }
                            val pointerId = down.id
                            fun scrubTo(x: Float) {
                                val inset = latestThumbExtent.toPx() / 2f
                                val trackWidth = (size.width - inset * 2f).coerceAtLeast(1f)
                                latestOnScrub(((x - inset) / trackWidth).coerceIn(0f, 1f))
                            }

                            latestOnScrubStart()
                            scrubTo(down.position.x)
                            down.consume()
                            try {
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == pointerId } ?: break
                                    scrubTo(change.position.x)
                                    change.consume()
                                    if (!change.pressed) break
                                }
                            } finally {
                                latestOnScrubEnd()
                            }
                        }
                    },
            ) {
                val inset = thumbExtent.toPx() / 2f
                val centerY = size.height / 2f
                val startX = inset
                val trackWidth = (size.width - inset * 2f).coerceAtLeast(0f)
                val playedX = startX + trackWidth * playedFraction
                val bufferedX = startX + trackWidth * bufferedFraction
                val strokeWidth = track.toPx()
                val halfGap = SegmentGap.toPx() / 2f
                val edges = listOf(0f) + segmentBoundaries.filter { it > 0f && it < 1f }.sorted() + listOf(1f)

                // Each segment is the same track clipped short of the boundaries, so the parts stay one bar with cuts in it.
                edges.zipWithNext().forEachIndexed { index, (from, to) ->
                    val left = if (index == 0) 0f else startX + trackWidth * from + halfGap
                    val right = if (index == edges.size - 2) size.width else startX + trackWidth * to - halfGap
                    clipRect(left = left, top = 0f, right = right, bottom = size.height) {
                        drawLine(
                            restColor,
                            Offset(startX, centerY),
                            Offset(startX + trackWidth, centerY),
                            strokeWidth,
                            StrokeCap.Round,
                        )
                        drawLine(
                            bufferedColor,
                            Offset(startX, centerY),
                            Offset(bufferedX, centerY),
                            strokeWidth,
                            StrokeCap.Round,
                        )
                        drawLine(
                            playedColor,
                            Offset(startX, centerY),
                            Offset(playedX, centerY),
                            strokeWidth,
                            StrokeCap.Round,
                        )
                    }
                    markedSpans.forEach { span ->
                        val markLeft = maxOf(left, startX + trackWidth * span.start)
                        val markRight = minOf(right, startX + trackWidth * span.end)
                        if (markRight <= markLeft) return@forEach
                        val mark = markColors[span.source.ordinal]
                        clipRect(left = markLeft, top = 0f, right = markRight, bottom = size.height) {
                            drawLine(
                                mark.copy(alpha = MARK_REST_ALPHA),
                                Offset(startX, centerY),
                                Offset(startX + trackWidth, centerY),
                                strokeWidth,
                                StrokeCap.Round,
                            )
                            drawLine(
                                mark,
                                Offset(startX, centerY),
                                Offset(playedX, centerY),
                                strokeWidth,
                                StrokeCap.Round,
                            )
                        }
                    }
                }
                if (halo > 0.dp) drawCircle(haloColor, radius = halo.toPx() / 2f, center = Offset(playedX, centerY))
                if (thumb > 0.dp) drawCircle(playedColor, radius = thumb.toPx() / 2f, center = Offset(playedX, centerY))
            }
            if (preview != null && previewAt != null) Box { preview() }
        },
    ) { measurables, constraints ->
        val bar = measurables[0].measure(constraints)
        val floating = measurables.getOrNull(1)?.measure(
            Constraints(maxWidth = minOf(bar.width, PreviewMaxWidth.roundToPx())),
        )
        layout(bar.width, bar.height) {
            bar.place(0, 0)
            if (floating != null && previewAt != null) {
                // Over the same point of the track as the thumb would be, as the scrub and hover fractions are
                val inset = thumbExtent.toPx() / 2f
                val trackWidth = (bar.width - inset * 2f).coerceAtLeast(1f)
                val centreX = inset + trackWidth * previewAt.coerceIn(0f, 1f)
                val x = (centreX - floating.width / 2f).roundToInt()
                    .coerceIn(0, (bar.width - floating.width).coerceAtLeast(0))
                floating.place(x, -floating.height - previewGap.roundToPx())
            }
        }
    }
}

private class TimelineGeometry(
    private val idleTrack: Dp,
    private val scrubbingTrack: Dp,
    private val idleThumb: Dp,
    private val scrubbingThumb: Dp,
    private val scrubbingHalo: Dp,
) {
    fun track(scrubbing: Boolean): Dp = if (scrubbing) scrubbingTrack else idleTrack
    fun thumb(scrubbing: Boolean): Dp = if (scrubbing) scrubbingThumb else idleThumb
    fun halo(scrubbing: Boolean): Dp = if (scrubbing) scrubbingHalo else 0.dp
}

private val FullGeometry = TimelineGeometry(
    idleTrack = 3.dp,
    scrubbingTrack = 5.dp,
    idleThumb = 10.dp,
    scrubbingThumb = 14.dp,
    scrubbingHalo = 24.dp,
)

private val CompactGeometry = TimelineGeometry(
    idleTrack = 3.dp,
    scrubbingTrack = 5.dp,
    idleThumb = 0.dp,
    scrubbingThumb = 12.dp,
    scrubbingHalo = 0.dp,
)

private val CompactHeight = 28.dp

// Wide enough to recognise a scene in, small enough to leave the picture around it in view.
private val PreviewMaxWidth = 320.dp
private val SegmentGap = 2.dp

private const val SIZE_ANIMATION_MS = 140
private const val HALO_ANIMATION_MS = 160
private const val REST_TRACK_ALPHA = 0.28f
private const val BUFFERED_TRACK_ALPHA = 0.42f
private const val HALO_ALPHA = 0.24f
private const val MARK_REST_ALPHA = 0.6f
