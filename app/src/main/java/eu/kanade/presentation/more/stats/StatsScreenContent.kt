package eu.kanade.presentation.more.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import eu.kanade.presentation.more.stats.components.StatisticsActivityCard
import eu.kanade.presentation.more.stats.components.StatisticsAdditionalInsightsCard
import eu.kanade.presentation.more.stats.components.StatisticsEarlierActivityCard
import eu.kanade.presentation.more.stats.components.StatisticsHeadlineCards
import eu.kanade.presentation.more.stats.components.StatisticsLibraryCard
import eu.kanade.presentation.more.stats.components.StatisticsProgressCard
import eu.kanade.presentation.more.stats.components.StatisticsTopTitlesCard
import eu.kanade.presentation.more.stats.components.color
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.presentation.more.stats.data.forType
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

@Composable
fun StatsScreenContent(
    state: StatsScreenState.Success,
    paddingValues: PaddingValues,
    onRangeSelected: (StatsRange) -> Unit,
    onTypeSelected: (EntryType?) -> Unit,
    onNavigateActivity: (Int) -> Unit,
    onShowToday: () -> Unit,
    onRetryActivity: () -> Unit,
    onOpenActivity: (EntryType?, StatsTrendPoint) -> Unit,
    onOpenEntry: (Long) -> Unit,
    onOpenEarlierActivity: (EntryType?, Long?) -> Unit,
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
                onNavigateActivity = onNavigateActivity,
                onShowToday = onShowToday,
                onRetryActivity = onRetryActivity,
                onOpenActivity = onOpenActivity,
                onOpenEntry = onOpenEntry,
                onOpenEarlierActivity = onOpenEarlierActivity,
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
    onNavigateActivity: (Int) -> Unit,
    onShowToday: () -> Unit,
    onRetryActivity: () -> Unit,
    onOpenActivity: (EntryType?, StatsTrendPoint) -> Unit,
    onOpenEntry: (Long) -> Unit,
    onOpenEarlierActivity: (EntryType?, Long?) -> Unit,
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
        visibleActivity?.completionCount?.let(NumberFormat.getIntegerInstance()::format) ?: "—"
    }
    val thirdHeadlineLabel = selectedStatsType?.let { stringResource(it.consumedUnitLabel) }
        ?: stringResource(
            if (visibleActivity?.window?.isLatest == false) {
                MR.strings.statistics_ending_streak
            } else {
                MR.strings.statistics_current_streak
            },
        )
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
                onNavigateByBuckets = onNavigateActivity,
                onToday = onShowToday,
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
            )
        }
        if (selectedStatsType != null) {
            state.library.insightsByType[selectedStatsType.type]?.let { insights ->
                item {
                    StatisticsAdditionalInsightsCard(
                        typeLabel = stringResource(selectedStatsType.displayName),
                        sessionCount = visibleActivity?.sessionCount,
                        averageSessionDurationMillis = visibleActivity?.averageSessionDurationMillis,
                        longestSessionDurationMillis = visibleActivity?.longestSessionDurationMillis,
                        activeDays = visibleActivity?.activeDays,
                        library = insights,
                        formatDuration = formatter,
                    )
                }
            }
        }
        if (visibleActivity?.earlierDurationMillis?.let { it > 0L } == true) {
            item {
                val beforeDate = visibleActivity.trackingStartedAtEpochMillis?.let { startedAt ->
                    val date = Instant.ofEpochMilli(startedAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                    stringResource(MR.strings.statistics_before_date, date)
                }
                StatisticsEarlierActivityCard(
                    duration = formatter(visibleActivity.earlierDurationMillis),
                    beforeDate = beforeDate,
                    onClick = {
                        onOpenEarlierActivity(selectedType, visibleActivity.trackingStartedAtEpochMillis)
                    },
                )
            }
        }
    }
}
