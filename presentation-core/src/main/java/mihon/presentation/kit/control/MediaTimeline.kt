package mihon.presentation.kit.control

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme

/**
 * A playback position bar laid over video: the played part in the primary colour with a thumb, then the buffered
 * part and the rest of the track in the media colours. [played] and [buffered] are fractions of the whole, 0..1.
 *
 * Pressing the bar starts a scrub: [onScrubStart], then [onScrub] with the touched fraction as the finger moves, then
 * [onScrubEnd] when it lifts or the gesture is cancelled. While [scrubbing] the track and thumb grow. A disabled bar,
 * such as before the duration is known, swallows touches without scrubbing. [compact] is the slim bar of a small
 * player, whose thumb only shows while scrubbing.
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
    enabled: Boolean = true,
    compact: Boolean = false,
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
    val height = if (compact) CompactHeight else thumbExtent + FullVerticalClearance

    val playedFraction = played.coerceIn(0f, 1f)
    val bufferedFraction = buffered.coerceIn(playedFraction, 1f)
    val colors = KatariTheme.colors
    val restColor = colors.onMedia.copy(alpha = REST_TRACK_ALPHA)
    val bufferedColor = colors.onMedia.copy(alpha = BUFFERED_TRACK_ALPHA)
    val playedColor = colors.primary
    val haloColor = colors.primary.copy(alpha = HALO_ALPHA)

    val latestEnabled by rememberUpdatedState(enabled)
    val latestThumbExtent by rememberUpdatedState(thumbExtent)
    val latestOnScrubStart by rememberUpdatedState(onScrubStart)
    val latestOnScrub by rememberUpdatedState(onScrub)
    val latestOnScrubEnd by rememberUpdatedState(onScrubEnd)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
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

        drawLine(restColor, Offset(startX, centerY), Offset(startX + trackWidth, centerY), strokeWidth, StrokeCap.Round)
        drawLine(bufferedColor, Offset(startX, centerY), Offset(bufferedX, centerY), strokeWidth, StrokeCap.Round)
        drawLine(playedColor, Offset(startX, centerY), Offset(playedX, centerY), strokeWidth, StrokeCap.Round)
        if (halo > 0.dp) drawCircle(haloColor, radius = halo.toPx() / 2f, center = Offset(playedX, centerY))
        if (thumb > 0.dp) drawCircle(playedColor, radius = thumb.toPx() / 2f, center = Offset(playedX, centerY))
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

/** Room above and below the full bar's thumb, so the bar is easy to grab. */
private val FullVerticalClearance = 8.dp

private val CompactHeight = 28.dp

private const val SIZE_ANIMATION_MS = 140
private const val HALO_ANIMATION_MS = 160
private const val REST_TRACK_ALPHA = 0.28f
private const val BUFFERED_TRACK_ALPHA = 0.42f
private const val HALO_ALPHA = 0.24f
