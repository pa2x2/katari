package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.roundToInt

@Composable
internal fun StatisticsTrendChart(
    points: List<StatsTrendPoint>,
    types: List<StatsType>,
    typeLabels: Map<EntryType, String>,
    formatDate: (StatsTrendPoint) -> String,
    formatDuration: (Long) -> String,
    onOpenActivity: (StatsTrendPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeColors = types.associate { it.type to it.accent.color() }
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val maximum = points.maxOfOrNull(StatsTrendPoint::totalDurationMillis)?.coerceAtLeast(1L) ?: 1L
    var selectedIndex by remember(points) { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }
    var exactValuesVisible by remember { mutableStateOf(false) }
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

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .pointerInput(points) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (points.isNotEmpty()) {
                            selectedIndex = trendPointIndexForPosition(
                                positionX = down.position.x,
                                width = size.width.toFloat(),
                                pointCount = points.size,
                                horizontalInset = CHART_HORIZONTAL_INSET.toPx(),
                            )
                        }
                        var change = down
                        while (change.pressed) {
                            val event = awaitPointerEvent()
                            change = event.changes.firstOrNull() ?: break
                            if (change.pressed && points.isNotEmpty()) {
                                selectedIndex = trendPointIndexForPosition(
                                    positionX = change.position.x,
                                    width = size.width.toFloat(),
                                    pointCount = points.size,
                                    horizontalInset = CHART_HORIZONTAL_INSET.toPx(),
                                )
                                change.consume()
                            }
                        }
                    }
                },
        ) {
            val chartHeight = size.height - 8.dp.toPx()
            val horizontalInset = CHART_HORIZONTAL_INSET.toPx().coerceAtMost(size.width / 2f)
            val chartWidth = (size.width - horizontalInset * 2f).coerceAtLeast(0f)
            repeat(4) { index ->
                val y = chartHeight * index / 3f
                drawLine(
                    color = Color(outlineVariant.value).copy(alpha = 0.45f),
                    start = Offset(horizontalInset, y),
                    end = Offset(size.width - horizontalInset, y),
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
            var lowerValues = List(points.size) { 0L }
            types.forEach { type ->
                val upperValues = points.mapIndexed { index, point ->
                    lowerValues[index] + (point.durationByType[type.type] ?: 0L)
                }
                val area = Path().apply {
                    points.indices.forEach { index ->
                        val y = chartHeight * (1f - upperValues[index].toFloat() / maximum.toFloat())
                        if (index == 0) moveTo(x(index), y) else lineTo(x(index), y)
                    }
                    points.indices.reversed().forEach { index ->
                        val y = chartHeight * (1f - lowerValues[index].toFloat() / maximum.toFloat())
                        lineTo(x(index), y)
                    }
                    close()
                }
                drawPath(area, typeColors.getValue(type.type).copy(alpha = 0.32f))
                lowerValues = upperValues
            }
            val outline = Path().apply {
                points.forEachIndexed { index, point ->
                    val y = chartHeight * (1f - point.totalDurationMillis.toFloat() / maximum.toFloat())
                    if (index == 0) moveTo(x(index), y) else lineTo(x(index), y)
                }
            }
            drawPath(
                path = outline,
                color = if (types.size == 1) typeColors.getValue(types.single().type) else primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
            if (selectedIndex in points.indices) {
                val selected = points[selectedIndex]
                val selectedX = x(selectedIndex)
                val selectedY = chartHeight * (1f - selected.totalDurationMillis.toFloat() / maximum.toFloat())
                drawLine(
                    color = onSurfaceVariant.copy(alpha = 0.55f),
                    start = Offset(selectedX, 0f),
                    end = Offset(selectedX, chartHeight),
                    strokeWidth = 1.dp.toPx(),
                )
                drawCircle(surfaceColor, 6.dp.toPx(), Offset(selectedX, selectedY))
                drawCircle(primaryColor, 4.dp.toPx(), Offset(selectedX, selectedY))
            }
        }

        if (points.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDate(points.first()), style = MaterialTheme.typography.labelSmall)
                if (points.size >
                    2
                ) {
                    Text(formatDate(points[points.lastIndex / 2]), style = MaterialTheme.typography.labelSmall)
                }
                Text(formatDate(points.last()), style = MaterialTheme.typography.labelSmall)
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

        TextButton(onClick = { exactValuesVisible = !exactValuesVisible }) {
            Icon(
                imageVector = if (exactValuesVisible) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(
                    if (exactValuesVisible) {
                        MR.strings.statistics_hide_exact_values
                    } else {
                        MR.strings.statistics_show_exact_values
                    },
                ),
            )
        }
        if (exactValuesVisible) {
            points.forEach { point ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatDate(point), style = MaterialTheme.typography.bodySmall)
                        Text(formatDuration(point.totalDurationMillis), style = MaterialTheme.typography.labelLarge)
                    }
                    val contributions = types.mapNotNull { type ->
                        (point.durationByType[type.type] ?: 0L).takeIf { it > 0L }?.let { duration ->
                            "${typeLabels.getValue(type.type)} ${formatDuration(duration)}"
                        }
                    }
                    if (contributions.isNotEmpty()) {
                        Text(
                            text = contributions.joinToString(separator = " · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

internal fun trendPointIndexForPosition(
    positionX: Float,
    width: Float,
    pointCount: Int,
    horizontalInset: Float,
): Int {
    if (pointCount <= 1 || width <= 0f) return 0
    val inset = horizontalInset.coerceIn(0f, width / 2f)
    val chartWidth = (width - inset * 2f).coerceAtLeast(1f)
    val fraction = ((positionX - inset) / chartWidth).coerceIn(0f, 1f)
    return (fraction * (pointCount - 1)).roundToInt()
}

private val CHART_HORIZONTAL_INSET = 12.dp
