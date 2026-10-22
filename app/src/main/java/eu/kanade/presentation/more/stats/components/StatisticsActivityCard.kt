package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import eu.kanade.presentation.more.stats.ActivityState
import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsTrendGranularity
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.StatisticsMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.DashboardCard
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun StatisticsActivityCard(
    state: ActivityState,
    activity: StatsActivity?,
    types: List<StatsType>,
    formatter: (Long) -> String,
    onNavigateByBuckets: (Int) -> Unit,
    onNavigateByWindows: (Int) -> Unit,
    onShowToday: () -> Unit,
    onShowLifetime: () -> Unit,
    onRetry: () -> Unit,
    onOpenActivity: (StatsTrendPoint) -> Unit,
) {
    when (state) {
        is ActivityState.Loading -> InitialActivityCard(state.target, onRetry)
        is ActivityState.Failed -> InitialActivityCard(state.target, onRetry, failed = true)
        is ActivityState.Available -> {
            val visibleActivity = activity ?: state.data
            SettledActivityCard(
                state = state,
                activity = visibleActivity,
                types = types,
                formatter = formatter,
                onNavigateByBuckets = onNavigateByBuckets,
                onNavigateByWindows = onNavigateByWindows,
                onShowToday = onShowToday,
                onShowLifetime = onShowLifetime,
                onRetry = onRetry,
                onOpenActivity = onOpenActivity,
            )
        }
    }
}

@Composable
private fun InitialActivityCard(
    target: StatsActivityWindow,
    onRetry: () -> Unit,
    failed: Boolean = false,
) {
    val targetLabel = formatStatisticsWindow(target)
    DashboardCard {
        if (failed) {
            Row(
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(MR.strings.statistics_could_not_load_period, targetLabel),
                    modifier = Modifier.weight(1f),
                )
                Button(label = stringResource(MR.strings.action_retry), onClick = onRetry, style = ButtonStyle.Text)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(StatisticsMetrics.chartHeight),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LoadingIndicator()
            }
        }
    }
}

@Composable
private fun SettledActivityCard(
    state: ActivityState.Available,
    activity: StatsActivity,
    types: List<StatsType>,
    formatter: (Long) -> String,
    onNavigateByBuckets: (Int) -> Unit,
    onNavigateByWindows: (Int) -> Unit,
    onShowToday: () -> Unit,
    onShowLifetime: () -> Unit,
    onRetry: () -> Unit,
    onOpenActivity: (StatsTrendPoint) -> Unit,
) {
    val window = activity.window
    val isFinite = window.range != StatsRange.ALL
    val isAllRange = window.range == StatsRange.ALL
    val hasYearDrilldown = isAllRange &&
        activity.trendGranularity == StatsTrendGranularity.YEAR &&
        activity.allRangeMonthlyTrend.isNotEmpty()
    var selectedYear by rememberSaveable(
        activity.trackingStartDate,
        activity.window.endDate,
    ) {
        mutableStateOf<Int?>(null)
    }
    LaunchedEffect(hasYearDrilldown) {
        if (!hasYearDrilldown) selectedYear = null
    }
    val availableYears = remember(activity.trend) { activity.trend.map { it.startDate.year } }
    val drilledYear = selectedYear?.takeIf { hasYearDrilldown && it in availableYears }
    val displayedPoints = drilledYear?.let { year ->
        activity.allRangeMonthlyTrend.filter { it.startDate.year == year }
    } ?: activity.trend
    val isYearOverview = hasYearDrilldown && drilledYear == null
    val isLatest = window.isLatest
    val isPending = state.loadingTarget != null
    val canNavigateOlder = isFinite &&
        activity.trackingStartDate?.let(window.endDate::isAfter) == true &&
        !isPending
    val canNavigateNewer = isFinite && !isLatest && !isPending
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    val axisDatePattern = when {
        drilledYear != null -> "MMM"
        else -> when (window.range) {
            StatsRange.SEVEN_DAYS -> "EEE"
            StatsRange.THIRTY_DAYS -> "MMM d"
            StatsRange.ONE_YEAR -> "MMM yy"
            StatsRange.ALL -> if (
                activity.trendGranularity == StatsTrendGranularity.YEAR
            ) {
                "yyyy"
            } else {
                "MMM yyyy"
            }
        }
    }
    val axisDateFormatter = remember(axisDatePattern, locale) {
        DateTimeFormatter.ofPattern(axisDatePattern, locale)
    }
    // Axis labels abbreviate months to "Sep 26", which reads as a day once it leaves the axis.
    val isMonthly = drilledYear != null || activity.trendGranularity == StatsTrendGranularity.MONTH
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("LLLL yyyy", locale) }
    // The All total lives in the summary and Lifetime cards; only a drilled year gets its own title.
    val cardTitle = drilledYear?.let { stringResource(MR.strings.statistics_year_activity, it) }
    val bucketLabel = stringResource(
        when {
            drilledYear != null -> MR.strings.statistics_monthly_total
            else -> when (activity.trendGranularity) {
                StatsTrendGranularity.DAY -> MR.strings.statistics_daily_total
                StatsTrendGranularity.WEEK -> MR.strings.statistics_weekly_total
                StatsTrendGranularity.MONTH -> MR.strings.statistics_monthly_total
                StatsTrendGranularity.YEAR -> MR.strings.statistics_yearly_total
            }
        },
    )

    val labels = types.associate { it.type to stringResource(it.displayName) }
    val typeColors = types.associate { it.type to it.accent.color() }
    val trackedStart = activity.trackingStartDate
    // Activity screens start at tracking; earlier days of a partly tracked bucket have no records.
    val openTracked = { point: StatsTrendPoint ->
        onOpenActivity(
            if (trackedStart != null && point.startDate.isBefore(trackedStart)) {
                point.copy(startDate = trackedStart)
            } else {
                point
            },
        )
    }
    val formatPoint = { point: StatsTrendPoint ->
        if (isYearOverview) {
            point.startDate.year.toString()
        } else if (point.startDate == point.endDate) {
            point.startDate.format(dateFormatter)
        } else {
            "${point.startDate.format(dateFormatter)} – ${point.endDate.format(dateFormatter)}"
        }
    }
    val notTrackedLabel = stringResource(MR.strings.statistics_not_tracked)
    val hasDisplayedActivity = displayedPoints.any { it.totalDurationMillis > 0L }
    val showsChart = !(isAllRange && drilledYear == null && !hasDisplayedActivity)
    val selectionKey = buildString {
        append(displayedPoints.firstOrNull()?.startDate)
        append(':')
        append(displayedPoints.lastOrNull()?.endDate)
        types.forEach { append(':').append(it.type.name) }
    }
    // Nothing is selected until the user picks a bar; the footer then sums up the whole period.
    var selectedStartDate by rememberSaveable(selectionKey) { mutableStateOf<String?>(null) }
    val selected = displayedPoints.firstOrNull { it.startDate.toString() == selectedStartDate }

    DashboardCard(
        title = cardTitle,
        footer = if (showsChart && displayedPoints.isNotEmpty()) {
            {
                if (selected != null) {
                    StatisticsTrendSelection(
                        selected = selected,
                        types = types,
                        typeLabels = labels,
                        typeColors = typeColors,
                        formattedDate = formatPoint(selected),
                        formattedDuration = formatter(selected.totalDurationMillis),
                        notTrackedLabel = notTrackedLabel,
                        formatDuration = formatter,
                        actionLabel = if (isYearOverview) {
                            stringResource(MR.strings.statistics_view_year, selected.startDate.year)
                        } else {
                            null
                        },
                        onOpenActivity = { point ->
                            if (isYearOverview) {
                                selectedYear = point.startDate.year
                            } else {
                                openTracked(point)
                            }
                        },
                        onClearSelection = { selectedStartDate = null },
                    )
                } else {
                    StatisticsTrendPeriodSummary(
                        summary = remember(displayedPoints) { summarizeTrendPeriod(displayedPoints) },
                        types = types,
                        typeLabels = labels,
                        typeColors = typeColors,
                        formatDuration = formatter,
                        formatBusiest = { point ->
                            when {
                                isYearOverview -> point.startDate.year.toString()
                                isMonthly -> point.startDate.format(monthFormatter)
                                else -> point.startDate.format(dateFormatter)
                            }
                        },
                        onOpen = {
                            openTracked(
                                StatsTrendPoint(
                                    startDate = displayedPoints.first().startDate,
                                    endDate = displayedPoints.last().endDate,
                                    durationByType = emptyMap(),
                                ),
                            )
                        },
                    )
                }
            }
        } else {
            null
        },
    ) {
        if (isAllRange) {
            drilledYear?.let { year ->
                AllRangeYearNavigation(
                    year = year,
                    previousEnabled = availableYears.firstOrNull()?.let { year > it } == true,
                    nextEnabled = availableYears.lastOrNull()?.let { year < it } == true,
                    onAllActivity = { selectedYear = null },
                    onPrevious = { selectedYear = year - 1 },
                    onNext = { selectedYear = year + 1 },
                )
            }
        } else {
            StatisticsWindowNavigation(
                window = window,
                olderEnabled = canNavigateOlder,
                newerEnabled = canNavigateNewer,
                onOlder = { onNavigateByWindows(1) },
                onNewer = { onNavigateByWindows(-1) },
                onToday = onShowToday,
            )
        }
        Spacer(
            Modifier.height(
                if (isAllRange &&
                    drilledYear == null
                ) {
                    KatariTheme.sizes.none
                } else {
                    KatariTheme.spacing.l
                },
            ),
        )
        if (!showsChart) {
            AllRangeEmptyActivity()
        } else {
            Box {
                StatisticsTrendChart(
                    points = displayedPoints,
                    navigationPoints = if (drilledYear == null) activity.navigationTrend else displayedPoints,
                    types = types,
                    typeLabels = labels,
                    formatDate = formatPoint,
                    formatAxisDate = { point -> point.startDate.format(axisDateFormatter) },
                    formatDuration = formatter,
                    notTrackedLabel = notTrackedLabel,
                    previousPointLabel = stringResource(MR.strings.statistics_previous_data_point),
                    nextPointLabel = stringResource(MR.strings.statistics_next_data_point),
                    bucketLabel = bucketLabel,
                    canNavigateOlder = canNavigateOlder && drilledYear == null,
                    canNavigateNewer = canNavigateNewer && drilledYear == null,
                    navigationPending = isPending,
                    onNavigateByBuckets = onNavigateByBuckets,
                    selectedStartDate = selectedStartDate,
                    onSelect = { selectedStartDate = it },
                )
                StatisticsActivityRequestStatus(
                    state = state,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
            if (trackedStart != null && displayedPoints.any { !it.isTracked }) {
                StatisticsTrackingStartRow(
                    trackingStartDate = trackedStart,
                    onShowLifetime = onShowLifetime.takeUnless { isAllRange },
                )
            }
        }
    }
}
