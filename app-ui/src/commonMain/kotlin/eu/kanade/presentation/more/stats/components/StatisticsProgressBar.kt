package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import eu.kanade.presentation.more.stats.data.StatsProgress
import mihon.presentation.kit.content.chartBar
import mihon.presentation.kit.theme.KatariTheme

/** Progress stages from least to most consumed; the stacked bar and its legends share this order. */
internal fun StatsProgress.stageCounts(): List<Int> = listOf(notStarted, inProgress, caughtUp, completed)

/** One hue per stage, deepening towards completion so the bar reads left to right as progress. */
@Composable
internal fun statisticsProgressStageColors(color: Color): List<Color> {
    val empty = KatariTheme.chart.track
    return STAGE_FRACTIONS.map { lerp(empty, color, it) }
}

@Composable
internal fun StatisticsProgressBar(
    progress: StatsProgress,
    color: Color,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = statisticsProgressStageColors(color)
    val track = KatariTheme.chart.track
    val counts = progress.stageCounts()
    Canvas(modifier.fillMaxWidth().height(height).chartBar()) {
        drawRect(track)
        val total = counts.sum()
        if (total == 0) return@Canvas
        var left = 0f
        counts.forEachIndexed { index, count ->
            val width = size.width * count / total
            drawRect(colors[index], topLeft = Offset(left, 0f), size = Size(width, size.height))
            left += width
        }
    }
}

private val STAGE_FRACTIONS = listOf(0.3f, 0.55f, 0.8f, 1f)
