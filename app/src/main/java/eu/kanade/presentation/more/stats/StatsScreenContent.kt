package eu.kanade.presentation.more.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.StatisticsAdditionalInsightsCard
import eu.kanade.presentation.more.stats.components.StatisticsHeadlineCards
import eu.kanade.presentation.more.stats.components.StatisticsLibraryCard
import eu.kanade.presentation.more.stats.components.StatisticsLibraryCoverageCard
import eu.kanade.presentation.more.stats.components.StatisticsProgressCard
import eu.kanade.presentation.more.stats.components.StatisticsSectionCard
import eu.kanade.presentation.more.stats.components.StatisticsTopTitlesCard
import eu.kanade.presentation.more.stats.components.StatisticsTrendChart
import eu.kanade.presentation.more.stats.components.color
import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsTrackingCoverage
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun StatsScreenContent(
    state: StatsScreenState.Success,
    paddingValues: PaddingValues,
    onRangeSelected: (StatsRange) -> Unit,
    onTypeSelected: (EntryType?) -> Unit,
    onRetryActivity: () -> Unit,
    onOpenActivity: (EntryType?, StatsTrendPoint) -> Unit,
    onOpenEntry: (Long) -> Unit,
    onOpenLibrary: (EntryType?) -> Unit,
    onOpenOfflineTitles: (EntryType) -> Unit,
    onOpenTrackedTitles: (EntryType) -> Unit,
) {
    val pages = remember(state.types) { listOf<EntryType?>(null) + state.types.map(StatsType::type) }
    val selectedPage = pages.indexOf(state.selectedType).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = selectedPage) { pages.size }
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current

    LaunchedEffect(state.profileId, state.selectedType, pages) {
        if (pagerState.currentPage != selectedPage) {
            pagerState.scrollToPage(selectedPage)
        }
    }
    LaunchedEffect(pagerState, pages) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page -> onTypeSelected(pages[page]) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = paddingValues.calculateTopPadding(),
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection),
            ),
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 0.dp,
        ) {
            pages.forEachIndexed { index, type ->
                val label = if (type == null) {
                    stringResource(MR.strings.label_overview_section)
                } else {
                    stringResource(state.types.first { it.type == type }.displayName)
                }
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { TabText(label) },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        StatisticsRangeSelector(
            selected = state.range,
            onSelected = onRangeSelected,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            key = { pages[it]?.name ?: "overview" },
        ) { page ->
            StatisticsPage(
                state = state,
                selectedType = pages[page],
                bottomPadding = paddingValues.calculateBottomPadding(),
                onRetryActivity = onRetryActivity,
                onOpenActivity = onOpenActivity,
                onOpenEntry = onOpenEntry,
                onOpenLibrary = onOpenLibrary,
                onOpenOfflineTitles = onOpenOfflineTitles,
                onOpenTrackedTitles = onOpenTrackedTitles,
            )
        }
    }
}

@Composable
private fun StatisticsRangeSelector(
    selected: StatsRange,
    onSelected: (StatsRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ranges = StatsRange.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        ranges.forEachIndexed { index, range ->
            SegmentedButton(
                selected = selected == range,
                onClick = { onSelected(range) },
                shape = SegmentedButtonDefaults.itemShape(index, ranges.size),
                label = {
                    Text(
                        stringResource(
                            when (range) {
                                StatsRange.SEVEN_DAYS -> MR.strings.statistics_range_7_days
                                StatsRange.THIRTY_DAYS -> MR.strings.statistics_range_30_days
                                StatsRange.ONE_YEAR -> MR.strings.statistics_range_1_year
                                StatsRange.ALL -> MR.strings.all
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun StatisticsPage(
    state: StatsScreenState.Success,
    selectedType: EntryType?,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onRetryActivity: () -> Unit,
    onOpenActivity: (EntryType?, StatsTrendPoint) -> Unit,
    onOpenEntry: (Long) -> Unit,
    onOpenLibrary: (EntryType?) -> Unit,
    onOpenOfflineTitles: (EntryType) -> Unit,
    onOpenTrackedTitles: (EntryType) -> Unit,
) {
    val visibleTypes = state.types.filter { selectedType == null || it.type == selectedType }
    val titleCounts = state.library.titlesByType.filterKeys { selectedType == null || it == selectedType }
    val titleCount = if (selectedType == null) state.library.totalTitles else titleCounts[selectedType] ?: 0
    val progress = if (selectedType == null) state.library.progress else state.library.progressByType[selectedType]
    val activity = (state.activity as? ActivityState.Available)?.data
    val formatter = rememberStatisticsDurationFormatter()
    val visibleActivity = activity?.forType(selectedType)
    val streakDays = when {
        activity == null -> null
        selectedType == null -> activity.currentStreakDays
        else -> activity.currentStreakDaysByType[selectedType] ?: 0
    }
    val selectedStatsType = state.types.firstOrNull { it.type == selectedType }
    val thirdHeadlineValue = if (selectedStatsType == null) {
        streakDays?.let { pluralStringResource(MR.plurals.day, it, it) } ?: "—"
    } else {
        NumberFormat.getIntegerInstance().format(visibleActivity?.completionCount ?: 0L)
    }
    val thirdHeadlineLabel = selectedStatsType?.let { stringResource(it.consumedUnitLabel) }
        ?: stringResource(MR.strings.statistics_current_streak)
    val emptyType = selectedStatsType != null &&
        titleCount == 0 &&
        state.activity is ActivityState.Available &&
        visibleActivity?.totalDurationMillis == 0L &&
        visibleActivity.completionCount == 0L &&
        visibleActivity.earlierDurationMillis == 0L

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding + 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (state.incognito) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = stringResource(MR.strings.statistics_incognito_active),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
        if (emptyType) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().fillParentMaxHeight(0.7f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = selectedStatsType.icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = selectedStatsType.accent.color(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(
                            MR.strings.statistics_no_type_stats,
                            stringResource(selectedStatsType.displayName),
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(MR.strings.statistics_empty_type_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@LazyColumn
        }
        item {
            StatisticsHeadlineCards(
                time = visibleActivity?.let { formatter(it.totalDurationMillis) } ?: "—",
                titles = titleCount,
                thirdValue = thirdHeadlineValue,
                thirdLabel = thirdHeadlineLabel,
                timeIcon = Icons.Outlined.Schedule,
                titlesIcon = Icons.Outlined.CollectionsBookmark,
                thirdIcon = selectedStatsType?.icon ?: Icons.Outlined.LocalFireDepartment,
            )
        }
        item {
            StatisticsActivityCard(
                state = state.activity,
                activity = visibleActivity,
                types = visibleTypes,
                formatter = formatter,
                onRetry = onRetryActivity,
                onOpenActivity = { point -> onOpenActivity(selectedType, point) },
            )
        }
        if (visibleActivity?.topTitles?.isNotEmpty() == true) {
            item {
                StatisticsTopTitlesCard(
                    titles = visibleActivity.topTitles,
                    typesById = state.types.associateBy(StatsType::type),
                    formatDuration = formatter,
                    onTitleClick = onOpenEntry,
                )
            }
        }
        item {
            StatisticsProgressCard(progress)
        }
        item {
            StatisticsLibraryCard(
                titleCounts = titleCounts,
                types = visibleTypes,
                onSeeTitles = { onOpenLibrary(selectedType) },
            )
        }
        selectedType?.let { type ->
            state.library.coverageByType[type]?.let { coverage ->
                item {
                    StatisticsLibraryCoverageCard(
                        coverage = coverage,
                        onSeeOfflineTitles = coverage.offline
                            ?.takeIf { it.partlyOfflineTitles + it.fullyOfflineTitles > 0 }
                            ?.let { { onOpenOfflineTitles(type) } },
                        onSeeTrackedTitles = (coverage.tracking as? StatsTrackingCoverage.Connected)
                            ?.takeIf { it.trackedTitles > 0 }
                            ?.let { { onOpenTrackedTitles(type) } },
                    )
                }
            }
        }
        if (selectedStatsType != null) {
            state.library.insightsByType[selectedStatsType.type]?.let { insights ->
                item {
                    StatisticsAdditionalInsightsCard(
                        typeLabel = stringResource(selectedStatsType.displayName),
                        sessionCount = visibleActivity?.sessionCount ?: 0L,
                        averageSessionDurationMillis = visibleActivity?.averageSessionDurationMillis ?: 0L,
                        longestSessionDurationMillis = visibleActivity?.longestSessionDurationMillis ?: 0L,
                        activeDays = visibleActivity?.activeDays ?: 0,
                        library = insights,
                        formatDuration = formatter,
                    )
                }
            }
        }
        if (visibleActivity?.earlierDurationMillis?.let { it > 0L } == true) {
            item {
                StatisticsSectionCard(stringResource(MR.strings.statistics_earlier_activity)) {
                    Text(
                        text = formatter(visibleActivity.earlierDurationMillis),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    visibleActivity.trackingStartedAtEpochMillis?.let { startedAt ->
                        val date = Instant.ofEpochMilli(startedAt)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                        Text(
                            text = stringResource(MR.strings.statistics_before_date, date),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsActivityCard(
    state: ActivityState,
    activity: StatsActivity?,
    types: List<StatsType>,
    formatter: (Long) -> String,
    onRetry: () -> Unit,
    onOpenActivity: (StatsTrendPoint) -> Unit,
) {
    when (state) {
        ActivityState.Loading -> StatisticsSectionCard(stringResource(MR.strings.statistics_activity)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
            }
        }
        ActivityState.Failed -> StatisticsSectionCard(stringResource(MR.strings.statistics_activity)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(MR.strings.statistics_could_not_load_activity))
                TextButton(onClick = onRetry) { Text(stringResource(MR.strings.action_retry)) }
            }
        }
        is ActivityState.Available -> StatisticsSectionCard(stringResource(MR.strings.statistics_activity)) {
            if (activity == null || activity.totalDurationMillis <= 0L) {
                Text(
                    text = stringResource(MR.strings.statistics_no_activity),
                    modifier = Modifier.padding(vertical = 44.dp).align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val labels = types.associate { it.type to stringResource(it.displayName) }
                val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                StatisticsTrendChart(
                    points = activity.trend,
                    types = types,
                    typeLabels = labels,
                    formatDate = { point ->
                        if (point.startDate == point.endDate) {
                            point.startDate.format(dateFormatter)
                        } else {
                            "${point.startDate.format(dateFormatter)} – ${point.endDate.format(dateFormatter)}"
                        }
                    },
                    formatDuration = formatter,
                    onOpenActivity = onOpenActivity,
                )
            }
            activity?.trackingStartedAtEpochMillis?.let { startedAt ->
                val recordedDate = Instant.ofEpochMilli(startedAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                Text(
                    text = stringResource(MR.strings.statistics_recorded_since, recordedDate),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun StatsActivity.forType(type: EntryType?): StatsActivity {
    if (type == null) return this
    return copy(
        totalDurationMillis = trend.sumOf { it.durationByType[type] ?: 0L },
        completionCount = completionCountByType[type] ?: 0L,
        completionCountByType = mapOf(type to (completionCountByType[type] ?: 0L)),
        sessionCount = sessionCountByType[type] ?: 0L,
        sessionCountByType = mapOf(type to (sessionCountByType[type] ?: 0L)),
        averageSessionDurationMillis = averageSessionDurationByType[type] ?: 0L,
        averageSessionDurationByType = mapOf(type to (averageSessionDurationByType[type] ?: 0L)),
        longestSessionDurationMillis = longestSessionDurationByType[type] ?: 0L,
        longestSessionDurationByType = mapOf(type to (longestSessionDurationByType[type] ?: 0L)),
        activeDays = activeDaysByType[type] ?: 0,
        activeDaysByType = mapOf(type to (activeDaysByType[type] ?: 0)),
        trend = trend.map { point ->
            point.copy(durationByType = mapOf(type to (point.durationByType[type] ?: 0L)))
        },
        topTitles = topTitles.filter { it.type == type },
        earlierDurationMillis = earlierDurationByType[type] ?: 0L,
        earlierDurationByType = mapOf(type to (earlierDurationByType[type] ?: 0L)),
    )
}

@Composable
private fun rememberStatisticsDurationFormatter(): (Long) -> String {
    val locale = Locale.getDefault()
    return remember(locale) {
        val numbers = NumberFormat.getIntegerInstance(locale)
        val formatter: (Long) -> String = { durationMillis ->
            val totalMinutes = durationMillis.coerceAtLeast(0L) / 60_000L
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            when {
                hours > 0L && minutes > 0L -> "${numbers.format(hours)}h ${numbers.format(minutes)}m"
                hours > 0L -> "${numbers.format(hours)}h"
                else -> "${numbers.format(minutes)}m"
            }
        }
        formatter
    }
}
