package eu.kanade.presentation.more.stats.recap.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.recap.components.LocalRecapPalette
import kotlin.math.roundToLong

/**
 * How far step [order] of the page's entrance has played, eased from 0 to 1. Steps start [STEP_MILLIS] apart, so a
 * page's parts arrive one after another; a page's last step, the fifth, lands about 2.5 seconds in. The value settles
 * at 1 when the step ends, so what reads it stops being redrawn.
 */
@Composable
internal fun rememberRecapEntrance(
    order: Int,
    durationMillis: Int = RISE_MILLIS,
    easing: Easing = RiseEasing,
): State<Float> {
    val clock = LocalRecapClock.current
    return remember(clock, order, durationMillis, easing) {
        derivedStateOf {
            if (clock.entranceSkipped) return@derivedStateOf 1f
            val played = (clock.pageMillis - order * STEP_MILLIS).toFloat() / durationMillis
            easing.transform(played.coerceIn(0f, 1f))
        }
    }
}

/** Brings an element in at step [order] of the page's entrance: it rises and fades in. */
@Composable
internal fun Modifier.recapReveal(order: Int = 0): Modifier {
    val progress = rememberRecapEntrance(order)
    val rise = 28.dp
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * rise.toPx()
    }
}

/** How far a count-up at step [order] has gone, 0 to 1, for anything that should move in step with the number. */
@Composable
internal fun rememberRecapCount(order: Int): State<Float> = rememberRecapEntrance(order, COUNT_MILLIS, CountEasing)

/** A number that counts up from zero at step [order] of the page's entrance, slowing as it lands on [value]. */
@Composable
internal fun RecapCountUp(
    value: Long,
    format: (Long) -> String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalRecapPalette.current.ink,
    order: Int = 0,
    autoSize: TextAutoSize? = null,
) {
    val progress by rememberRecapCount(order)
    Text(
        text = format((value * progress).roundToLong()),
        style = style,
        color = color,
        maxLines = if (autoSize != null) 1 else Int.MAX_VALUE,
        autoSize = autoSize,
        modifier = modifier,
    )
}

/** How long charts take to grow in: longer than text rises, since the eye follows the shape filling. */
internal const val RECAP_GROW_MILLIS = 1_200

private const val RISE_MILLIS = 800
private const val STEP_MILLIS = 350
private const val COUNT_MILLIS = 1_500
private val RiseEasing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)
private val CountEasing = CubicBezierEasing(0.15f, 0.8f, 0.25f, 1f)
