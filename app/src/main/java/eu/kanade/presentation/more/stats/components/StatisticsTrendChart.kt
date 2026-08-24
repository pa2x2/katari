package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun StatisticsTrendChart(
    points: List<StatsTrendPoint>,
    navigationPoints: List<StatsTrendPoint>,
    types: List<StatsType>,
    typeLabels: Map<EntryType, String>,
    formatDate: (StatsTrendPoint) -> String,
    formatAxisDate: (StatsTrendPoint) -> String,
    formatDuration: (Long) -> String,
    notTrackedLabel: String,
    previousPointLabel: String,
    nextPointLabel: String,
    canNavigateOlder: Boolean,
    canNavigateNewer: Boolean,
    navigationPending: Boolean,
    onNavigateByBuckets: (Int) -> Unit,
    onOpenActivity: (StatsTrendPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeColors = types.associate { it.type to it.accent.color() }
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val rawMaximum = niceTrendMaximum(
        navigationPoints.maxOfOrNull(StatsTrendPoint::totalDurationMillis) ?: 0L,
    )
    var retainedMaximum by remember(types.map(StatsType::type), points.size) {
        mutableLongStateOf(rawMaximum)
    }
    val maximum = maxOf(rawMaximum, retainedMaximum)
    LaunchedEffect(rawMaximum) {
        retainedMaximum = maxOf(retainedMaximum, rawMaximum)
    }

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
    var plotOffsetPx by remember(selectionKey) { mutableFloatStateOf(0f) }
    var liveBucketShift by remember(selectionKey) { mutableStateOf(0) }
    LaunchedEffect(navigationPending) {
        if (!navigationPending) {
            plotOffsetPx = 0f
            liveBucketShift = 0
        }
    }

    val tickIndices = trendTickIndices(points.size)
    val visibleStartIndex = navigationPoints.indexOfFirst {
        it.bucketStartDate == points.firstOrNull()?.bucketStartDate
    }.coerceAtLeast(0)
    val maximumVisibleStartIndex = (navigationPoints.size - points.size).coerceAtLeast(0)
    val liveVisibleStartIndex = (visibleStartIndex - liveBucketShift)
        .coerceIn(0, maximumVisibleStartIndex)
    val liveTickPoints = tickIndices.mapNotNull { tickIndex ->
        navigationPoints.getOrNull(liveVisibleStartIndex + tickIndex)
    }
    val liveVisiblePoints = navigationPoints.subList(
        liveVisibleStartIndex,
        (liveVisibleStartIndex + points.size).coerceAtMost(navigationPoints.size),
    )
    val activityLabel = stringResource(MR.strings.statistics_activity)
    val selectedPoint = points.getOrNull(selectedIndex)
    val selectedSummary = selectedPoint?.let { point ->
        if (point.isTracked) {
            "${formatDate(point)} · ${formatDuration(point.totalDurationMillis)}"
        } else {
            "${formatDate(point)} · $notTrackedLabel"
        }
    }.orEmpty()
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
        if (selectedSummary.isNotEmpty()) append(". ").append(selectedSummary)
    }
    val accessibilityActions = buildList {
        if (selectedIndex > 0) {
            add(
                CustomAccessibilityAction(previousPointLabel) {
                    selectedStartDate = points[selectedIndex - 1].startDate.toString()
                    true
                },
            )
        }
        if (selectedIndex < points.lastIndex) {
            add(
                CustomAccessibilityAction(nextPointLabel) {
                    selectedStartDate = points[selectedIndex + 1].startDate.toString()
                    true
                },
            )
        }
    }

    Column(
        modifier = modifier.semantics {
            contentDescription = semanticsText
            customActions = accessibilityActions
        },
    ) {
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
            Box(
                modifier = Modifier.weight(1f).height(CHART_HEIGHT),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(
                            points,
                            navigationPoints,
                            canNavigateOlder,
                            canNavigateNewer,
                            navigationPending,
                        ) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val touchSlop = viewConfiguration.touchSlop
                                var capturedHorizontalDrag = false
                                var latestPosition = down.position
                                var latestTime = down.uptimeMillis

                                fun updatePlotOffset(rawOffset: Float) {
                                    plotOffsetPx = resistedOffset(
                                        rawOffset,
                                        size.width.toFloat(),
                                        canNavigateOlder,
                                        canNavigateNewer,
                                    )
                                    val plotWidth = (
                                        size.width - CHART_HORIZONTAL_INSET.toPx() * 2f
                                        ).coerceAtLeast(1f)
                                    val pointSpacing = if (points.size <= 1) {
                                        plotWidth
                                    } else {
                                        plotWidth / points.lastIndex
                                    }
                                    liveBucketShift = (plotOffsetPx / pointSpacing).roundToInt()
                                        .coerceIn(
                                            visibleStartIndex - maximumVisibleStartIndex,
                                            visibleStartIndex,
                                        )
                                }

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    latestPosition = change.position
                                    latestTime = change.uptimeMillis
                                    val displacement = change.position - down.position

                                    if (!capturedHorizontalDrag) {
                                        if (change.isConsumed) break
                                        if (!change.pressed) {
                                            if (displacement.getDistance() <= touchSlop && points.isNotEmpty()) {
                                                val index = trendPointIndexForPosition(
                                                    positionX = change.position.x,
                                                    width = size.width.toFloat(),
                                                    pointCount = points.size,
                                                    horizontalInset = CHART_HORIZONTAL_INSET.toPx(),
                                                )
                                                selectedStartDate = points[index].startDate.toString()
                                            }
                                            break
                                        }
                                        if (
                                            abs(displacement.y) > touchSlop &&
                                            abs(displacement.y) >= abs(displacement.x)
                                        ) {
                                            break
                                        }
                                        if (
                                            abs(displacement.x) > touchSlop &&
                                            abs(displacement.x) > abs(displacement.y) &&
                                            !navigationPending &&
                                            (canNavigateOlder || canNavigateNewer)
                                        ) {
                                            capturedHorizontalDrag = true
                                            change.consume()
                                            updatePlotOffset(displacement.x)
                                        }
                                    } else {
                                        updatePlotOffset(displacement.x)
                                        change.consume()
                                        if (!change.pressed) break
                                    }
                                }

                                if (capturedHorizontalDrag) {
                                    val elapsedSeconds = ((latestTime - down.uptimeMillis).coerceAtLeast(1L)) / 1_000f
                                    val displacement = latestPosition.x - down.position.x
                                    val velocity = displacement / elapsedSeconds
                                    val projectedOffset = plotOffsetPx + velocity * FLING_PROJECTION_SECONDS
                                    val plotWidth = (size.width - CHART_HORIZONTAL_INSET.toPx() * 2f).coerceAtLeast(1f)
                                    val pointSpacing = if (points.size <= 1) {
                                        plotWidth
                                    } else {
                                        plotWidth / points.lastIndex
                                    }
                                    var bucketCount = (projectedOffset / pointSpacing).roundToInt()
                                        .coerceIn(-points.size.coerceAtLeast(1), points.size.coerceAtLeast(1))
                                    if (bucketCount > 0 && !canNavigateOlder) bucketCount = 0
                                    if (bucketCount < 0 && !canNavigateNewer) bucketCount = 0
                                    if (bucketCount == 0) {
                                        plotOffsetPx = 0f
                                        liveBucketShift = 0
                                    } else {
                                        plotOffsetPx = bucketCount * pointSpacing
                                        liveBucketShift = bucketCount
                                        onNavigateByBuckets(bucketCount)
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
                    val visibleX = { index: Int ->
                        if (points.size == 1) {
                            size.width / 2f
                        } else {
                            horizontalInset + chartWidth * index / points.lastIndex
                        }
                    }
                    tickIndices.forEach { index ->
                        drawLine(
                            color = Color(outlineVariant.value).copy(alpha = 0.2f),
                            start = Offset(visibleX(index), plotTop),
                            end = Offset(visibleX(index), plotBottom),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    val pointSpacing = if (points.size <= 1) {
                        chartWidth
                    } else {
                        chartWidth / points.lastIndex
                    }
                    val barWidth = (pointSpacing * 0.62f).coerceIn(
                        MINIMUM_BAR_WIDTH.toPx(),
                        MAXIMUM_BAR_WIDTH.toPx(),
                    )
                    val navigationX = { index: Int ->
                        horizontalInset + pointSpacing * (index - visibleStartIndex)
                    }

                    clipRect(horizontalInset, plotTop, size.width - horizontalInset, plotBottom) {
                        translate(left = plotOffsetPx) {
                            navigationPoints.forEachIndexed { index, point ->
                                val untrackedFraction = point.untrackedFraction()
                                if (untrackedFraction > 0f) {
                                    val left = navigationX(index) - pointSpacing / 2f
                                    val pointRight = navigationX(index) + pointSpacing / 2f
                                    val right = left + (pointRight - left) * untrackedFraction
                                    drawRect(
                                        color = Color(outlineVariant.value).copy(alpha = 0.18f),
                                        topLeft = Offset(left, plotTop),
                                        size = Size((right - left).coerceAtLeast(1f), plotHeight),
                                    )
                                    clipRect(left, plotTop, right, plotBottom) {
                                        var hatchX = left - plotHeight
                                        while (hatchX < right) {
                                            drawLine(
                                                color = onSurfaceVariant.copy(alpha = 0.16f),
                                                start = Offset(hatchX, plotBottom),
                                                end = Offset(hatchX + plotHeight, plotTop),
                                                strokeWidth = 1.dp.toPx(),
                                            )
                                            hatchX += 10.dp.toPx()
                                        }
                                    }
                                }
                            }
                            navigationPoints.forEachIndexed { index, point ->
                                if (!point.isTracked) return@forEachIndexed
                                var cumulative = 0L
                                types.forEach { type ->
                                    val duration = point.durationByType[type.type] ?: 0L
                                    if (duration > 0L) {
                                        val bottom = y(cumulative)
                                        cumulative += duration
                                        val top = y(cumulative)
                                        drawRect(
                                            color = typeColors.getValue(type.type).copy(alpha = 0.78f),
                                            topLeft = Offset(navigationX(index) - barWidth / 2f, top),
                                            size = Size(barWidth, (bottom - top).coerceAtLeast(1f)),
                                        )
                                    }
                                }
                            }

                            val trackedPaths = buildList {
                                var path: Path? = null
                                navigationPoints.forEachIndexed { index, point ->
                                    if (!point.isTracked) {
                                        path?.let(::add)
                                        path = null
                                    } else {
                                        val currentPath = path
                                        if (currentPath == null) {
                                            path = Path().apply {
                                                moveTo(navigationX(index), y(point.totalDurationMillis))
                                            }
                                        } else {
                                            currentPath.lineTo(navigationX(index), y(point.totalDurationMillis))
                                        }
                                    }
                                }
                                path?.let(::add)
                            }
                            trackedPaths.forEach { path ->
                                drawPath(
                                    path = path,
                                    color = if (types.size == 1) {
                                        typeColors.getValue(types.single().type)
                                    } else {
                                        primaryColor
                                    },
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                                )
                            }
                            if (selectedIndex in points.indices) {
                                val selected = points[selectedIndex]
                                val selectedNavigationIndex = navigationPoints.indexOfFirst {
                                    it.bucketStartDate == selected.bucketStartDate
                                }
                                val selectedX = navigationX(
                                    selectedNavigationIndex.takeIf { it >= 0 } ?: visibleStartIndex + selectedIndex,
                                )
                                val selectedY = y(selected.totalDurationMillis)
                                drawLine(
                                    color = onSurfaceVariant.copy(alpha = 0.55f),
                                    start = Offset(selectedX, plotTop),
                                    end = Offset(selectedX, plotBottom),
                                    strokeWidth = 1.dp.toPx(),
                                )
                                drawCircle(surfaceColor, 6.dp.toPx(), Offset(selectedX, selectedY))
                                drawCircle(
                                    color = if (selected.isTracked) primaryColor else onSurfaceVariant,
                                    radius = 4.dp.toPx(),
                                    center = Offset(selectedX, selectedY),
                                )
                            }
                        }
                    }
                }
                if (liveVisiblePoints.any { it.untrackedFraction() > 0f }) {
                    Text(
                        text = notTrackedLabel,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = CHART_HORIZONTAL_INSET + 4.dp, top = CHART_VERTICAL_INSET + 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        if (points.isNotEmpty()) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(Y_AXIS_WIDTH))
                Box(Modifier.weight(1f).clipToBounds()) {
                    Row(Modifier.fillMaxWidth()) {
                        liveTickPoints.forEachIndexed { tickPosition, point ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = when (tickPosition) {
                                    0 -> Alignment.CenterStart
                                    tickIndices.lastIndex -> Alignment.CenterEnd
                                    else -> Alignment.Center
                                },
                            ) {
                                Text(
                                    text = formatAxisDate(point),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
            val selected = points[selectedIndex.coerceIn(points.indices)]
            StatisticsTrendSelection(
                selected = selected,
                types = types,
                typeLabels = typeLabels,
                typeColors = typeColors,
                formattedDate = formatDate(selected),
                formattedDuration = formatDuration(selected.totalDurationMillis),
                notTrackedLabel = notTrackedLabel,
                formatDuration = formatDuration,
                onOpenActivity = onOpenActivity,
            )
        }
    }
}

private fun resistedOffset(
    rawOffset: Float,
    width: Float,
    canNavigateOlder: Boolean,
    canNavigateNewer: Boolean,
): Float {
    val bounded = rawOffset.coerceIn(-width, width)
    return when {
        bounded > 0f && !canNavigateOlder -> bounded * BOUNDARY_RESISTANCE
        bounded < 0f && !canNavigateNewer -> bounded * BOUNDARY_RESISTANCE
        else -> bounded
    }
}

private fun StatsTrendPoint.untrackedFraction(): Float {
    val trackedStart = trackedStartDate ?: return 1f
    if (!trackedStart.isAfter(startDate)) return 0f
    val bucketDays = ChronoUnit.DAYS.between(startDate, endDate).toFloat() + 1f
    return (ChronoUnit.DAYS.between(startDate, trackedStart).toFloat() / bucketDays).coerceIn(0f, 1f)
}

private const val FLING_PROJECTION_SECONDS = 0.08f
private const val BOUNDARY_RESISTANCE = 0.2f
private val CHART_HORIZONTAL_INSET = 12.dp
private val CHART_VERTICAL_INSET = 6.dp
private val CHART_HEIGHT = 220.dp
private val Y_AXIS_WIDTH = 48.dp
private val MINIMUM_BAR_WIDTH = 1.dp
private val MAXIMUM_BAR_WIDTH = 24.dp
