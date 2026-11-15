package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.katari.core.platform.currentPlatformContext
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.ChevronLeft
import app.katari.icons.materialsymbols.rounded.ChevronRight
import app.katari.icons.materialsymbols.rounded.Close
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.LeadFigure
import app.katari.presentation.kit.content.StatisticsMetrics
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.progress.LoadingIndicator
import app.katari.presentation.kit.surface.DashboardCard
import app.katari.presentation.kit.surface.DashboardCardFooter
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.more.stats.ActivityState
import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsTrendGranularity
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    val targetLabel = currentPlatformContext().formatStatisticsWindow(target)
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
    val locale = LocalLocale.current.platformLocale
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
    val platformContext = currentPlatformContext()
    val formatPoint = { point: StatsTrendPoint ->
        val withYear = point.startDate.year != LocalDate.now().year || point.endDate.year != LocalDate.now().year
        when {
            isYearOverview -> point.startDate.year.toString()
            point.startDate == point.endDate -> platformContext.formatStatisticsDay(point.startDate, withYear)
            else -> platformContext.formatStatisticsDays(point.startDate, point.endDate, withYear)
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
    // Nothing is selected until the user picks a bar; the card then reads out that bar instead of the period.
    var selectedStartDate by rememberSaveable(selectionKey) { mutableStateOf<String?>(null) }
    val selected = displayedPoints.firstOrNull { it.startDate.toString() == selectedStartDate }
    val period = remember(displayedPoints) { summarizeTrendPeriod(displayedPoints) }
    val periodLabel = when {
        drilledYear != null -> drilledYear.toString()
        isAllRange -> null
        else -> platformContext.formatStatisticsWindow(window)
    }

    DashboardCard(
        title = selected?.let(formatPoint) ?: periodLabel,
        actions = when {
            selected != null -> {
                {
                    IconButton(
                        icon = MaterialSymbols.Rounded.Close,
                        contentDescription = stringResource(MR.strings.statistics_show_whole_period),
                        onClick = { selectedStartDate = null },
                        tone = ContentTone.Secondary,
                    )
                }
            }
            isFinite -> {
                {
                    if (!isLatest) {
                        Button(
                            label = stringResource(MR.strings.statistics_today),
                            onClick = onShowToday,
                            style = ButtonStyle.Text,
                        )
                    }
                    IconButton(
                        icon = MaterialSymbols.Rounded.ChevronLeft,
                        contentDescription = stringResource(MR.strings.statistics_older_activity),
                        onClick = { onNavigateByWindows(1) },
                        enabled = canNavigateOlder,
                    )
                    IconButton(
                        icon = MaterialSymbols.Rounded.ChevronRight,
                        contentDescription = stringResource(MR.strings.statistics_newer_activity),
                        onClick = { onNavigateByWindows(-1) },
                        enabled = canNavigateNewer,
                    )
                }
            }
            drilledYear != null -> {
                {
                    Button(
                        label = stringResource(MR.strings.statistics_all_activity),
                        onClick = { selectedYear = null },
                        style = ButtonStyle.Text,
                    )
                    IconButton(
                        icon = MaterialSymbols.Rounded.ChevronLeft,
                        contentDescription = stringResource(MR.strings.statistics_previous_year),
                        onClick = { selectedYear = drilledYear - 1 },
                        enabled = availableYears.firstOrNull()?.let { drilledYear > it } == true,
                    )
                    IconButton(
                        icon = MaterialSymbols.Rounded.ChevronRight,
                        contentDescription = stringResource(MR.strings.statistics_next_year),
                        onClick = { selectedYear = drilledYear + 1 },
                        enabled = availableYears.lastOrNull()?.let { drilledYear < it } == true,
                    )
                }
            }
            else -> null
        },
        footer = if (showsChart && displayedPoints.isNotEmpty()) {
            {
                if (selected != null) {
                    PeriodCardFooter(
                        label = when {
                            isYearOverview -> stringResource(MR.strings.statistics_view_year, selected.startDate.year)
                            selected.startDate == selected.endDate -> stringResource(MR.strings.statistics_see_day)
                            else -> stringResource(MR.strings.statistics_see_days)
                        },
                        enabled = isTrendSelectionActionable(selected, hasAlternateAction = isYearOverview),
                        onClick = {
                            if (isYearOverview) selectedYear = selected.startDate.year else openTracked(selected)
                        },
                    )
                } else {
                    PeriodCardFooter(
                        label = stringResource(MR.strings.statistics_see_period_activity),
                        enabled = period.totalDurationMillis > 0L,
                        onClick = {
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
        if (!showsChart) {
            AllRangeEmptyActivity()
            return@DashboardCard
        }
        LeadFigure(
            value = when {
                selected == null -> formatter(period.totalDurationMillis)
                selected.isTracked -> formatter(selected.totalDurationMillis)
                else -> notTrackedLabel
            },
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            detail = if (selected == null && drilledYear == null) {
                { StatisticsPeriodComparison(activity, formatter) }
            } else {
                null
            },
        )
        Box(Modifier.padding(top = KatariTheme.spacing.l)) {
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
        StatisticsTypeSplit(
            shares = statisticsTypeShares(
                durationByType = when {
                    selected == null -> period.durationByType
                    selected.isTracked -> selected.durationByType
                    else -> emptyMap()
                },
                types = types,
                typeLabels = labels,
                typeColors = typeColors,
                formatDuration = formatter,
            ),
            modifier = Modifier.padding(top = KatariTheme.spacing.m),
        )
        if (trackedStart != null && displayedPoints.any { !it.isTracked }) {
            StatisticsTrackingStartRow(
                trackingStartDate = trackedStart,
                onShowLifetime = onShowLifetime.takeUnless { isAllRange },
            )
        }
    }
}

/** Opens what the card shows, the period or the selected bar, in the activity list. */
@Composable
private fun PeriodCardFooter(label: String, enabled: Boolean, onClick: () -> Unit) {
    DashboardCardFooter(onClick = onClick, enabled = enabled) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, modifier = Modifier.weight(1f), role = TextRole.Label)
            if (enabled) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                    tone = ContentTone.Secondary,
                )
            }
        }
    }
}

/** A bar opens its activity when it was tracked and has some, or when it leads somewhere else, such as into a year. */
internal fun isTrendSelectionActionable(
    selected: StatsTrendPoint,
    hasAlternateAction: Boolean,
): Boolean = selected.isTracked && (selected.totalDurationMillis > 0L || hasAlternateAction)
