package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun StatisticsTrendChart(
    points: List<StatsTrendPoint>,
    types: List<StatsType>,
    typeLabels: Map<EntryType, String>,
    formatDate: (StatsTrendPoint) -> String,
    formatAxisDate: (StatsTrendPoint) -> String,
    formatDuration: (Long) -> String,
    onOpenActivity: (StatsTrendPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeColors = types.associate { it.type to it.accent.color() }
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val maximum = niceTrendMaximum(
        points.maxOfOrNull(StatsTrendPoint::totalDurationMillis) ?: 0L,
    )
    val selectionKey = buildString {
        append(points.firstOrNull()?.startDate)
        append(':')
        append(points.lastOrNull()?.endDate)
        types.forEach { append(':').append(it.type.name) }
    }
    var selectedStartDate by rememberSaveable(selectionKey) {
        mutableStateOf(points.lastOrNull()?.startDate?.toString())
    }
    val selectedIndex = points.indexOfFirst { it.startDate.toString() == selectedStartDate }
        .takeIf { it >= 0 }
        ?: points.lastIndex.coerceAtLeast(0)
    val tickIndices = trendTickIndices(points.size)
    val activityLabel = stringResource(MR.strings.statistics_activity)
    val semanticsText = buildString {
        append(activityLabel)
        append(". ")
        append(formatDuration(points.sumOf(StatsTrendPoint::totalDurationMillis)))
        types.forEach { type ->
            append(", ")
            append(typeLabels[type.type] ?: type.type.name)
            append(' ')
            append(formatDuration(points.sumOf { it.durationByType[type.type] ?: 0L }))
        }
    }

    Column(modifier = modifier.semantics { contentDescription = semanticsText }) {
        if (types.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                types.forEach { type ->
                    Row {
                        Surface(
                            modifier = Modifier.padding(top = 5.dp).width(10.dp).height(10.dp),
                            color = typeColors.getValue(type.type),
                            shape = CircleShape,
                            content = {},
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = typeLabels.getValue(type.type),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
            Column(
                modifier = Modifier.width(Y_AXIS_WIDTH).height(CHART_HEIGHT),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(formatDuration(maximum), style = MaterialTheme.typography.labelSmall)
                Text(formatDuration(maximum / 2L), style = MaterialTheme.typography.labelSmall)
                Text(formatDuration(0L), style = MaterialTheme.typography.labelSmall)
            }
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(CHART_HEIGHT)
                    .pointerInput(points) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (points.isNotEmpty()) {
                                val index = trendPointIndexForPosition(
                                    positionX = down.position.x,
                                    width = size.width.toFloat(),
                                    pointCount = points.size,
                                    horizontalInset = CHART_HORIZONTAL_INSET.toPx(),
                                )
                                selectedStartDate = points[index].startDate.toString()
                            }
                            var change = down
                            while (change.pressed) {
                                val event = awaitPointerEvent()
                                change = event.changes.firstOrNull() ?: break
                                if (change.pressed && points.isNotEmpty()) {
                                    val index = trendPointIndexForPosition(
                                        positionX = change.position.x,
                                        width = size.width.toFloat(),
                                        pointCount = points.size,
                                        horizontalInset = CHART_HORIZONTAL_INSET.toPx(),
                                    )
                                    selectedStartDate = points[index].startDate.toString()
                                    change.consume()
                                }
                            }
                        }
                    },
            ) {
                val plotTop = CHART_VERTICAL_INSET.toPx()
                val plotBottom = size.height - CHART_VERTICAL_INSET.toPx()
                val plotHeight = (plotBottom - plotTop).coerceAtLeast(1f)
                val horizontalInset = CHART_HORIZONTAL_INSET.toPx().coerceAtMost(size.width / 2f)
                val chartWidth = (size.width - horizontalInset * 2f).coerceAtLeast(0f)
                val y = { value: Long ->
                    plotBottom - plotHeight * value.coerceIn(0L, maximum).toFloat() / maximum.toFloat()
                }
                listOf(maximum, maximum / 2L, 0L).forEach { value ->
                    drawLine(
                        color = Color(outlineVariant.value).copy(alpha = 0.45f),
                        start = Offset(horizontalInset, y(value)),
                        end = Offset(size.width - horizontalInset, y(value)),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                if (points.isEmpty()) return@Canvas
                val x = { index: Int ->
                    if (points.size == 1) {
                        size.width / 2f
                    } else {
                        horizontalInset + chartWidth * index / points.lastIndex
                    }
                }
                tickIndices.forEach { index ->
                    drawLine(
                        color = Color(outlineVariant.value).copy(alpha = 0.2f),
                        start = Offset(x(index), plotTop),
                        end = Offset(x(index), plotBottom),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                val pointSpacing = if (points.size <= 1) chartWidth else chartWidth / points.lastIndex
                val barWidth = (pointSpacing * 0.62f)
                    .coerceIn(MINIMUM_BAR_WIDTH.toPx(), MAXIMUM_BAR_WIDTH.toPx())
                points.forEachIndexed { index, point ->
                    var cumulative = 0L
                    types.forEach { type ->
                        val duration = point.durationByType[type.type] ?: 0L
                        if (duration > 0L) {
                            val bottom = y(cumulative)
                            cumulative += duration
                            val top = y(cumulative)
                            drawRect(
                                color = typeColors.getValue(type.type).copy(alpha = 0.78f),
                                topLeft = Offset(x(index) - barWidth / 2f, top),
                                size = Size(barWidth, (bottom - top).coerceAtLeast(1f)),
                            )
                        }
                    }
                }
                val outline = Path().apply {
                    points.forEachIndexed { index, point ->
                        if (index == 0) {
                            moveTo(x(index), y(point.totalDurationMillis))
                        } else {
                            lineTo(x(index), y(point.totalDurationMillis))
                        }
                    }
                }
                drawPath(
                    path = outline,
                    color = if (types.size == 1) typeColors.getValue(types.single().type) else primaryColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
                if (selectedIndex in points.indices) {
                    val selected = points[selectedIndex]
                    val selectedX = x(selectedIndex)
                    val selectedY = y(selected.totalDurationMillis)
                    drawLine(
                        color = onSurfaceVariant.copy(alpha = 0.55f),
                        start = Offset(selectedX, plotTop),
                        end = Offset(selectedX, plotBottom),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawCircle(surfaceColor, 6.dp.toPx(), Offset(selectedX, selectedY))
                    drawCircle(primaryColor, 4.dp.toPx(), Offset(selectedX, selectedY))
                }
            }
        }

        if (points.isNotEmpty()) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(Y_AXIS_WIDTH))
                Row(Modifier.weight(1f)) {
                    tickIndices.forEachIndexed { tickPosition, pointIndex ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = when (tickPosition) {
                                0 -> Alignment.CenterStart
                                tickIndices.lastIndex -> Alignment.CenterEnd
                                else -> Alignment.Center
                            },
                        ) {
                            Text(formatAxisDate(points[pointIndex]), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            val selected = points[selectedIndex.coerceIn(points.indices)]
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = "${formatDate(selected)} · ${formatDuration(selected.totalDurationMillis)}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (types.size > 1) {
                        types.forEach { type ->
                            val duration = selected.durationByType[type.type] ?: 0L
                            if (duration > 0L) {
                                Text(
                                    text = "${typeLabels.getValue(type.type)} · ${formatDuration(duration)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            TextButton(
                onClick = { onOpenActivity(points[selectedIndex.coerceIn(points.indices)]) },
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(stringResource(MR.strings.statistics_see_activity))
            }
        }
    }
}

private val CHART_HORIZONTAL_INSET = 12.dp
private val CHART_VERTICAL_INSET = 6.dp
private val CHART_HEIGHT = 220.dp
private val Y_AXIS_WIDTH = 48.dp
private val MINIMUM_BAR_WIDTH = 1.dp
private val MAXIMUM_BAR_WIDTH = 24.dp
