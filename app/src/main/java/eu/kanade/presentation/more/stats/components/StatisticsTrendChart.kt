package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.presentation.kit.content.StatisticsMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
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
    bucketLabel: String,
    canNavigateOlder: Boolean,
    canNavigateNewer: Boolean,
    navigationPending: Boolean,
    onNavigateByBuckets: (Int) -> Unit,
    onOpenActivity: (StatsTrendPoint) -> Unit,
    periodSummary: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selectionActionLabel: @Composable (StatsTrendPoint) -> String? = { null },
) {
    val displayedNavigationPoints = remember(points, navigationPoints) {
        buildDisplayedNavigationTrend(points, navigationPoints)
    }
    val typeColors = types.associate { it.type to it.accent.color() }
    val chartColors = KatariTheme.colors.chart
    val surfaceColor = KatariTheme.colors.surface
    val primaryColor = KatariTheme.colors.primary
    val onSurfaceVariant = KatariTheme.colors.onSurfaceVariant
    val gridStroke = KatariTheme.sizes.border
    val dataHorizontalInsetPx = with(LocalDensity.current) {
        DATA_INSET.toPx()
    }
    val selectionKey = buildString {
        append(points.firstOrNull()?.startDate)
        append(':')
        append(points.lastOrNull()?.endDate)
        types.forEach { append(':').append(it.type.name) }
    }
    // Nothing is selected until the user picks a bar; the panel then summarizes the whole period.
    var selectedStartDate by rememberSaveable(selectionKey) { mutableStateOf<String?>(null) }
    val selectedIndex = points.indexOfFirst { it.startDate.toString() == selectedStartDate }
    val scrollStateKey = points.size to types.map(StatsType::type)
    var plotOffsetPx by remember(scrollStateKey) { mutableFloatStateOf(0f) }
    var pointSpacingPx by remember(scrollStateKey) { mutableFloatStateOf(0f) }
    var renderedWindowStart by remember(scrollStateKey) {
        mutableStateOf(points.firstOrNull()?.bucketStartDate)
    }
    var pendingNavigation by remember(scrollStateKey) {
        mutableStateOf<PendingTrendNavigation?>(null)
    }

    val tickIndices = trendTickIndices(points.size)
    val visibleStartIndex = navigationPoints.indexOfFirst {
        it.bucketStartDate == points.firstOrNull()?.bucketStartDate
    }.coerceAtLeast(0)
    val maximumVisibleStartIndex = (navigationPoints.size - points.size).coerceAtLeast(0)
    val currentWindowStart = points.firstOrNull()?.bucketStartDate
    val reconciledPlotOffsetPx = if (
        currentWindowStart != renderedWindowStart &&
        pointSpacingPx > 0f
    ) {
        val pending = pendingNavigation
        if (pending != null) {
            val previousWindowIndex = navigationPoints.indexOfFirst {
                it.bucketStartDate == pending.sourceWindowStart
            }
            val appliedBucketShift = if (previousWindowIndex >= 0) {
                previousWindowIndex - visibleStartIndex
            } else {
                pending.requestedBucketShift
            }
            (pending.restingOffsetBuckets - appliedBucketShift) * pointSpacingPx
        } else {
            0f
        }
    } else {
        null
    }
    // Draw the new window with its reconciled offset immediately; committing it only from an
    // effect would expose one frame where the new points still use the previous window's offset.
    val displayedPlotOffsetPx = reconciledPlotOffsetPx ?: plotOffsetPx
    SideEffect {
        if (reconciledPlotOffsetPx != null) {
            plotOffsetPx = reconciledPlotOffsetPx
            pendingNavigation = null
            renderedWindowStart = currentWindowStart
        }
    }
    val scrollableState = rememberScrollableState { delta ->
        if (navigationPending || pendingNavigation != null || pointSpacingPx <= 0f) {
            return@rememberScrollableState 0f
        }
        val oldestOffset = if (canNavigateOlder) {
            visibleStartIndex * pointSpacingPx
        } else {
            0f
        }
        val newestOffset = if (canNavigateNewer) {
            -(maximumVisibleStartIndex - visibleStartIndex) * pointSpacingPx
        } else {
            0f
        }
        val previousOffset = plotOffsetPx
        plotOffsetPx = (plotOffsetPx + delta).coerceIn(newestOffset, oldestOffset)
        plotOffsetPx - previousOffset
    }
    val liveBucketShift = if (pointSpacingPx > 0f) {
        (displayedPlotOffsetPx / pointSpacingPx).roundToInt().coerceIn(
            visibleStartIndex - maximumVisibleStartIndex,
            visibleStartIndex,
        )
    } else {
        0
    }
    val axisOffsetPx = displayedPlotOffsetPx - liveBucketShift * pointSpacingPx
    val liveVisibleStartIndex = (visibleStartIndex - liveBucketShift)
        .coerceIn(0, maximumVisibleStartIndex)
    val liveTickPoints = tickIndices.mapNotNull { tickIndex ->
        displayedNavigationPoints.getOrNull(liveVisibleStartIndex + tickIndex)
    }
    val liveVisiblePoints = displayedNavigationPoints.subList(
        liveVisibleStartIndex,
        (liveVisibleStartIndex + points.size).coerceAtMost(navigationPoints.size),
    )
    val durationAxis = remember(liveVisiblePoints) {
        buildTrendDurationAxis(
            liveVisiblePoints.maxOfOrNull(StatsTrendPoint::totalDurationMillis) ?: 0L,
        )
    }
    val maximum = durationAxis.maximumMillis

    LaunchedEffect(scrollableState, pointSpacingPx, navigationPending) {
        snapshotFlow { scrollableState.isScrollInProgress }
            .collect { isScrolling ->
                if (
                    isScrolling ||
                    navigationPending ||
                    pendingNavigation != null ||
                    pointSpacingPx <= 0f
                ) {
                    return@collect
                }
                val settled = settleTrendOffset(
                    offsetPx = plotOffsetPx,
                    pointSpacingPx = pointSpacingPx,
                    olderBucketCount = if (canNavigateOlder) visibleStartIndex else 0,
                    newerBucketCount = if (canNavigateNewer) {
                        maximumVisibleStartIndex - visibleStartIndex
                    } else {
                        0
                    },
                )
                if (settled.bucketShift != 0) {
                    pendingNavigation = PendingTrendNavigation(
                        sourceWindowStart = points.firstOrNull()?.bucketStartDate,
                        requestedBucketShift = settled.bucketShift,
                        restingOffsetBuckets = settled.bucketShift +
                            settled.residualOffsetPx / pointSpacingPx,
                    )
                    onNavigateByBuckets(settled.bucketShift)
                }
            }
    }
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
        val previousIndex = if (selectedIndex < 0) points.lastIndex else selectedIndex - 1
        val nextIndex = if (selectedIndex < 0) 0 else selectedIndex + 1
        points.getOrNull(previousIndex)?.let { previous ->
            add(
                CustomAccessibilityAction(previousPointLabel) {
                    selectedStartDate = previous.startDate.toString()
                    true
                },
            )
        }
        points.getOrNull(nextIndex)?.let { next ->
            add(
                CustomAccessibilityAction(nextPointLabel) {
                    selectedStartDate = next.startDate.toString()
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
        Text(
            text = bucketLabel,
            modifier = Modifier.fillMaxWidth(),
            role = TextRole.Badge,
            tone = ContentTone.Secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(KatariTheme.spacing.m))
        if (types.size > 1) {
            StatisticsTrendLegend(types, typeLabels, typeColors)
            Spacer(Modifier.height(KatariTheme.spacing.m))
        }

        Row(Modifier.fillMaxWidth().height(StatisticsMetrics.chartHeight)) {
            Column(
                modifier = Modifier.width(StatisticsMetrics.chartAxisWidth).height(StatisticsMetrics.chartHeight),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                durationAxis.ticksDescending.forEach { tick ->
                    Text(formatDuration(tick), role = TextRole.Badge)
                }
            }
            Box(
                modifier = Modifier.weight(1f).height(StatisticsMetrics.chartHeight),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { chartSize ->
                            val dataWidth = (chartSize.width - dataHorizontalInsetPx * 2f).coerceAtLeast(1f)
                            pointSpacingPx = if (points.size <= 1) {
                                dataWidth
                            } else {
                                dataWidth / points.lastIndex
                            }
                        }
                        .nestedScroll(StatisticsTrendChartPagerIsolation)
                        .scrollable(
                            state = scrollableState,
                            orientation = Orientation.Horizontal,
                            enabled = !navigationPending &&
                                pendingNavigation == null &&
                                (canNavigateOlder || canNavigateNewer),
                        )
                        .pointerInput(points) {
                            detectTapGestures { position ->
                                if (points.isNotEmpty()) {
                                    val index = trendPointIndexForPosition(
                                        positionX = position.x - displayedPlotOffsetPx,
                                        width = size.width.toFloat(),
                                        pointCount = points.size,
                                        horizontalInset = DATA_INSET.toPx(),
                                    )
                                    val tapped = points[index].startDate.toString()
                                    selectedStartDate = tapped.takeUnless { it == selectedStartDate }
                                }
                            }
                        },
                ) {
                    val plotTop = StatisticsMetrics.chartVerticalInset.toPx()
                    val plotBottom = size.height - StatisticsMetrics.chartVerticalInset.toPx()
                    val plotHeight = (plotBottom - plotTop).coerceAtLeast(1f)
                    val horizontalInset = StatisticsMetrics.chartSideInset.toPx().coerceAtMost(size.width / 2f)
                    val dataHorizontalInset = (horizontalInset + StatisticsMetrics.chartEdgePointInset.toPx())
                        .coerceAtMost(size.width / 2f)
                    val dataWidth = (size.width - dataHorizontalInset * 2f).coerceAtLeast(0f)
                    val y = { value: Long ->
                        plotBottom - plotHeight * value.coerceIn(0L, maximum).toFloat() / maximum.toFloat()
                    }
                    durationAxis.ticksDescending.forEach { value ->
                        drawLine(
                            color = chartColors.gridLine,
                            start = Offset(horizontalInset, y(value)),
                            end = Offset(size.width - horizontalInset, y(value)),
                            strokeWidth = gridStroke.toPx(),
                        )
                    }
                    listOf(horizontalInset, size.width - horizontalInset).forEach { x ->
                        drawLine(
                            color = chartColors.gridLine,
                            start = Offset(x, plotTop),
                            end = Offset(x, plotBottom),
                            strokeWidth = gridStroke.toPx(),
                        )
                    }
                    if (points.isEmpty()) return@Canvas
                    tickIndices.forEach { index ->
                        val tickX = trendPointCenterX(
                            index = index,
                            width = size.width,
                            pointCount = points.size,
                            horizontalInset = dataHorizontalInset,
                        )
                        drawLine(
                            color = chartColors.tickLine,
                            start = Offset(tickX + axisOffsetPx, plotTop),
                            end = Offset(tickX + axisOffsetPx, plotBottom),
                            strokeWidth = gridStroke.toPx(),
                        )
                    }
                    val pointSpacing = if (points.size <= 1) {
                        dataWidth
                    } else {
                        dataWidth / points.lastIndex
                    }
                    val barWidth = (pointSpacing * 0.62f).coerceIn(
                        StatisticsMetrics.barMinWidth.toPx(),
                        StatisticsMetrics.barMaxWidth.toPx(),
                    )
                    val navigationX = { index: Int ->
                        dataHorizontalInset + pointSpacing * (index - visibleStartIndex)
                    }

                    clipRect(horizontalInset, plotTop, size.width - horizontalInset, plotBottom) {
                        translate(left = displayedPlotOffsetPx) {
                            displayedNavigationPoints.forEachIndexed { index, point ->
                                if (point.isTracked) return@forEachIndexed
                                drawUntrackedHatch(
                                    left = navigationX(index) - pointSpacing / 2f,
                                    right = navigationX(index) + pointSpacing / 2f,
                                    top = plotTop,
                                    bottom = plotBottom,
                                    color = chartColors.hatch,
                                    stroke = gridStroke,
                                )
                            }
                            displayedNavigationPoints.forEachIndexed { index, point ->
                                if (!point.isTracked) return@forEachIndexed
                                var cumulative = 0L
                                types.forEach { type ->
                                    val duration = point.durationByType[type.type] ?: 0L
                                    if (duration > 0L) {
                                        val bottom = y(cumulative)
                                        cumulative += duration
                                        val top = y(cumulative)
                                        drawRect(
                                            color = chartColors.seriesFill(typeColors.getValue(type.type)),
                                            topLeft = Offset(navigationX(index) - barWidth / 2f, top),
                                            size = Size(barWidth, (bottom - top).coerceAtLeast(1f)),
                                        )
                                    }
                                }
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
                                    color = chartColors.selectionGuide,
                                    start = Offset(selectedX, plotTop),
                                    end = Offset(selectedX, plotBottom),
                                    strokeWidth = gridStroke.toPx(),
                                )
                                drawCircle(
                                    surfaceColor,
                                    StatisticsMetrics.selectionHaloRadius.toPx(),
                                    Offset(selectedX, selectedY),
                                )
                                drawCircle(
                                    color = if (selected.isTracked) primaryColor else onSurfaceVariant,
                                    radius = StatisticsMetrics.selectionDotRadius.toPx(),
                                    center = Offset(selectedX, selectedY),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (points.isNotEmpty()) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(StatisticsMetrics.chartAxisWidth))
                Box(Modifier.weight(1f).clipToBounds()) {
                    TrendAxisLabels(
                        labels = liveTickPoints.map(formatAxisDate),
                        tickIndices = tickIndices,
                        pointCount = points.size,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { translationX = axisOffsetPx },
                    )
                }
            }
            val selected = points.getOrNull(selectedIndex)
            if (selected == null) {
                periodSummary()
            } else {
                StatisticsTrendSelection(
                    selected = selected,
                    types = types,
                    typeLabels = typeLabels,
                    typeColors = typeColors,
                    formattedDate = formatDate(selected),
                    formattedDuration = formatDuration(selected.totalDurationMillis),
                    notTrackedLabel = notTrackedLabel,
                    formatDuration = formatDuration,
                    actionLabel = selectionActionLabel(selected),
                    onOpenActivity = onOpenActivity,
                    onClearSelection = { selectedStartDate = null },
                )
            }
        }
    }
}

@Composable
private fun TrendAxisLabels(
    labels: List<String>,
    tickIndices: List<Int>,
    pointCount: Int,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier,
        content = {
            labels.forEach { label ->
                Text(text = label, role = TextRole.Badge)
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        val horizontalInset = DATA_INSET.roundToPx().toFloat()
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                val centerX = trendPointCenterX(
                    index = tickIndices[index],
                    width = constraints.maxWidth.toFloat(),
                    pointCount = pointCount,
                    horizontalInset = horizontalInset,
                )
                placeable.place(
                    x = (centerX - placeable.width / 2f).roundToInt(),
                    y = 0,
                )
            }
        }
    }
}

/** Distance from the chart's edge to the first and last bar centres. */
private val DATA_INSET = StatisticsMetrics.chartSideInset + StatisticsMetrics.chartEdgePointInset
