package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.recap.motion.RECAP_GROW_MILLIS
import eu.kanade.presentation.more.stats.recap.motion.rememberRecapEntrance

/**
 * Vertical bars that grow in at step [order] of the page's entrance, the [highlight] one in full accent and the rest
 * dimmed.
 */
@Composable
internal fun RecapBars(
    values: List<Long>,
    labels: List<String>,
    highlight: Int,
    height: Dp,
    order: Int,
    modifier: Modifier = Modifier,
) {
    val palette = LocalRecapPalette.current
    val grow by rememberRecapEntrance(order, RECAP_GROW_MILLIS)
    val max = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier.fillMaxWidth().height(height),
    ) {
        values.forEachIndexed { index, value ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    if (value > 0L) {
                        val share = maxOf(MIN_SHARE, value.toFloat() / max) * grow
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(share.coerceIn(0.001f, 1f))
                                .background(
                                    color = if (index == highlight) {
                                        palette.accent
                                    } else {
                                        palette.accent.copy(alpha = 0.4f)
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                ),
                        )
                    }
                }
                RecapText(
                    text = labels[index],
                    style = RecapTypography.Tiny,
                    color = palette.muted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The least a bar shows, so a little time still reads as some; no time shows no bar. */
private const val MIN_SHARE = 0.03f
