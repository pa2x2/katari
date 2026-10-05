package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

/**
 * Brings an element in as the page's entrance plays: it rises and fades in, [order] steps after the first element,
 * so a page's parts arrive one after another rather than all at once.
 */
@Composable
internal fun Modifier.recapReveal(order: Int = 0): Modifier {
    val reveal = LocalRecapReveal.current
    val rise = 28.dp
    return graphicsLayer {
        val progress = stepProgress(reveal.value, order)
        alpha = progress
        translationY = (1f - progress) * rise.toPx()
    }
}

/** A number that counts up from zero as the page's entrance plays, landing on [value] when it ends. */
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
    val reveal = LocalRecapReveal.current
    val shown = (value * stepProgress(reveal.value, order)).roundToLong()
    Text(
        text = format(shown),
        style = style,
        color = color,
        maxLines = if (autoSize != null) 1 else Int.MAX_VALUE,
        autoSize = autoSize,
        modifier = modifier,
    )
}

/** How far step [order] of an entrance has played when the whole entrance is at [reveal]. */
private fun stepProgress(reveal: Float, order: Int): Float {
    val start = order * STEP_DELAY
    val local = ((reveal * (1f + STEPS * STEP_DELAY) - start)).coerceIn(0f, 1f)
    return FastOutSlowInEasing.transform(local)
}

/** Steps start this share of an element's own entrance apart. */
private const val STEP_DELAY = 0.18f

/** The entrance is stretched so up to this many steps after the first still finish with it. */
private const val STEPS = 5
