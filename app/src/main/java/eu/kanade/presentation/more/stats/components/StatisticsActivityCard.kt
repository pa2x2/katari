package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.ActivityState
import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsTrendGranularity
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import kotlinx.coroutines.delay
import tachiyomi.i18n.MR
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
    StatisticsSectionCard {
        if (failed) {
            Row(
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(MR.strings.statistics_could_not_load_period, targetLabel))
                TextButton(onClick = onRetry) { Text(stringResource(MR.strings.action_retry)) }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
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
    val allRangeTotal = if (drilledYear == null) {
        activity.totalDurationMillis
    } else {
        displayedPoints.sumOf(StatsTrendPoint::totalDurationMillis)
    }
    val cardTitle = when {
        drilledYear != null -> stringResource(MR.strings.statistics_year_activity, drilledYear)
        isAllRange -> stringResource(MR.strings.statistics_all_recorded_activity)
        else -> null
    }
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

    StatisticsSectionCard(
        title = cardTitle,
        contentSpacing = 0.dp,
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
                Spacer(Modifier.height(12.dp))
            }
            AllRangeSummary(
                totalDuration = formatter(allRangeTotal),
                year = drilledYear,
            )
        } else {
            ActivityWindowNavigation(
                window = window,
                olderEnabled = canNavigateOlder,
                newerEnabled = canNavigateNewer,
                onOlder = { onNavigateByBuckets(1) },
                onNewer = { onNavigateByBuckets(-1) },
            )
        }
        Spacer(Modifier.height(16.dp))
        val labels = types.associate { it.type to stringResource(it.displayName) }
        val notTrackedLabel = stringResource(MR.strings.statistics_not_tracked)
        val hasDisplayedActivity = displayedPoints.any { it.totalDurationMillis > 0L }
        if (isAllRange && drilledYear == null && !hasDisplayedActivity) {
            AllRangeEmptyActivity()
        } else {
            Box {
                StatisticsTrendChart(
                    points = displayedPoints,
                    navigationPoints = if (drilledYear == null) activity.navigationTrend else displayedPoints,
                    types = types,
                    typeLabels = labels,
                    formatDate = { point ->
                        if (isYearOverview) {
                            point.startDate.year.toString()
                        } else if (point.startDate == point.endDate) {
                            point.startDate.format(dateFormatter)
                        } else {
                            "${point.startDate.format(dateFormatter)} – ${point.endDate.format(dateFormatter)}"
                        }
                    },
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
                    selectionActionLabel = { point ->
                        if (isYearOverview) {
                            stringResource(MR.strings.statistics_view_year, point.startDate.year)
                        } else {
                            null
                        }
                    },
                    onOpenActivity = { point ->
                        if (isYearOverview) {
                            selectedYear = point.startDate.year
                        } else {
                            val trackedStart = activity.trackingStartDate
                            onOpenActivity(
                                if (trackedStart != null && point.startDate.isBefore(trackedStart)) {
                                    point.copy(startDate = trackedStart)
                                } else {
                                    point
                                },
                            )
                        }
                    },
                )
                ActivityRequestStatus(
                    state = state,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!hasDisplayedActivity) {
                    Text(
                        text = stringResource(MR.strings.statistics_no_activity),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityWindowNavigation(
    window: StatsActivityWindow,
    olderEnabled: Boolean,
    newerEnabled: Boolean,
    onOlder: () -> Unit,
    onNewer: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (window.range != StatsRange.ALL) {
            IconButton(enabled = olderEnabled, onClick = onOlder) {
                Icon(Icons.Outlined.ChevronLeft, stringResource(MR.strings.statistics_older_activity))
            }
        }
        Text(
            text = formatStatisticsWindow(window),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
        if (window.range != StatsRange.ALL) {
            IconButton(enabled = newerEnabled, onClick = onNewer) {
                Icon(Icons.Outlined.ChevronRight, stringResource(MR.strings.statistics_newer_activity))
            }
        }
    }
}

@Composable
private fun ActivityRequestStatus(
    state: ActivityState.Available,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingTarget = state.loadingTarget
    var visibleLoadingTarget by remember(loadingTarget) {
        mutableStateOf<StatsActivityWindow?>(null)
    }
    LaunchedEffect(loadingTarget) {
        if (loadingTarget != null) {
            delay(LOADING_STATUS_DELAY_MILLIS)
            visibleLoadingTarget = loadingTarget
        }
    }

    Box(
        modifier = modifier.fillMaxWidth().height(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        visibleLoadingTarget?.let { target ->
            Surface(
                shape = MaterialTheme.shapes.small,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(
                            MR.strings.statistics_loading_period,
                            formatStatisticsWindow(target),
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        state.failedTarget?.let { target ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            MR.strings.statistics_could_not_load_period,
                            formatStatisticsWindow(target),
                        ),
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text(stringResource(MR.strings.action_retry))
                    }
                }
            }
        }
    }
}

private const val LOADING_STATUS_DELAY_MILLIS = 500L
