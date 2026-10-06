package eu.kanade.presentation.more.stats.recap.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.recap.components.RecapCover
import eu.kanade.presentation.more.stats.recap.motion.LocalRecapClock
import eu.kanade.presentation.more.stats.recap.motion.recapReveal
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapTitle
import kotlin.math.ceil

/**
 * The period's covers as tilted rows drifting right to left, each at its own speed, without end. A row repeats its
 * covers to fill the width, so a handful of titles still makes a full wall. The wall fades out at its top and bottom
 * into the page's background.
 */
@Composable
internal fun RecapCoverWall(covers: List<StatisticsRecapTitle>, modifier: Modifier = Modifier) {
    val clock = LocalRecapClock.current
    val rows = remember(covers) { wallRows(covers) }
    Box(
        modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                clip = true
                alpha = WALL_ALPHA
            }
            .drawWithContent {
                drawContent()
                drawRect(FadeMask, blendMode = BlendMode.DstIn)
            },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(GAP),
            modifier = Modifier.align(Alignment.Center).requiredWidth(WALL_WIDTH).rotate(WALL_TILT),
        ) {
            rows.forEachIndexed { row, rowCovers ->
                val loop = (COVER_WIDTH + GAP) * rowCovers.size
                val copies = ceil(WALL_WIDTH / loop).toInt() + 1
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GAP),
                    modifier = Modifier
                        .wrapContentWidth(Alignment.Start, unbounded = true)
                        .recapReveal(row)
                        .graphicsLayer {
                            val travelled = ROW_STARTS[row].value + clock.storyMillis * ROW_SPEEDS[row] / 1000f
                            translationX = -(travelled % loop.value).dp.toPx()
                        },
                ) {
                    repeat(copies) { rowCovers.forEach { RecapCover(it, width = COVER_WIDTH) } }
                }
            }
        }
    }
}

/**
 * Splits [covers] into the wall's rows. With enough titles each shows once; with fewer, every row holds all of them,
 * starting a third further along than the row above so neighbouring rows don't line up.
 */
private fun <T> wallRows(covers: List<T>): List<List<T>> {
    if (covers.isEmpty()) return emptyList()
    if (covers.size >= ROWS * MIN_ROW_COVERS) return covers.chunked(ceil(covers.size / ROWS.toFloat()).toInt())
    return List(ROWS) { row -> List(covers.size) { covers[(it + row * covers.size / ROWS) % covers.size] } }
}

private const val ROWS = 3
private const val MIN_ROW_COVERS = 4
private const val WALL_ALPHA = 0.6f
private const val WALL_TILT = -8f
private val WALL_WIDTH = 470.dp
private val COVER_WIDTH = 84.dp
private val GAP = 10.dp

/** Where each row starts and how fast it drifts, in dp per second, so the rows move apart rather than as one. */
private val ROW_STARTS = listOf(0.dp, 36.dp, 18.dp)
private val ROW_SPEEDS = listOf(12f, 17f, 9f)

private val FadeMask = Brush.verticalGradient(
    0f to Color.Transparent,
    0.16f to Color.Black,
    0.72f to Color.Black,
    1f to Color.Transparent,
)
