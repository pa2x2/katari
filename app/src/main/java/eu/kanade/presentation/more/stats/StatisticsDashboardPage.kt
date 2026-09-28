package eu.kanade.presentation.more.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.stats.components.StatisticsActivityCard
import eu.kanade.presentation.more.stats.components.StatisticsActivityFeedback
import eu.kanade.presentation.more.stats.components.StatisticsActivityPatternsCard
import eu.kanade.presentation.more.stats.components.StatisticsActivitySummaryCards
import eu.kanade.presentation.more.stats.components.StatisticsCurrentLibraryHeader
import eu.kanade.presentation.more.stats.components.StatisticsLibraryCard
import eu.kanade.presentation.more.stats.components.StatisticsLibraryInsightsCard
import eu.kanade.presentation.more.stats.components.StatisticsLifetimeCard
import eu.kanade.presentation.more.stats.components.StatisticsPeriodRow
import eu.kanade.presentation.more.stats.components.StatisticsProgressCard
import eu.kanade.presentation.more.stats.components.StatisticsReadingCalendarCard
import eu.kanade.presentation.more.stats.components.StatisticsSectionCard
import eu.kanade.presentation.more.stats.components.StatisticsTopTitlesCard
import eu.kanade.presentation.more.stats.components.color
import eu.kanade.presentation.more.stats.components.formatStatisticsWindow
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.presentation.more.stats.data.forType
import eu.kanade.presentation.more.stats.layout.StatisticsCardGroup
import eu.kanade.presentation.more.stats.layout.statisticsLayoutTab
import eu.kanade.presentation.more.stats.layout.visibleSections
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.statistics.model.StatisticsCard
import tachiyomi.domain.statistics.model.StatisticsCardLayout
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun StatisticsDashboardPage(
    state: StatsScreenState.Success,
    selectedType: EntryType?,
    bottomPadding: Dp,
    onRangeSelected: (StatsRange) -> Unit,
    onTypeSelected: (EntryType?) -> Unit,
    onNavigateActivity: (Int) -> Unit,
    onShowToday: () -> Unit,
    onRetryActivity: () -> Unit,
    onOpenActivity: (EntryType?, StatsTrendPoint) -> Unit,
    onOpenEntry: (Long) -> Unit,
    onOpenEarlierActivity: (EntryType?) -> Unit,
    onOpenTopTitles: (EntryType?, StatsActivity) -> Unit,
) {
    val isOverview = selectedType == null
    val visibleTypes = state.types.filter { isOverview || it.type == selectedType }
    val titleCounts = state.library.titlesByType.filterKeys { isOverview || it == selectedType }
    val titleCount = if (isOverview) state.library.totalTitles else titleCounts[selectedType] ?: 0
    val progress = if (isOverview) state.library.progress else state.library.progressByType[selectedType]
    val activity = (state.activity as? ActivityState.Available)?.data
    val formatter = rememberStatisticsDurationFormatter()
    val visibleActivity = activity?.forType(selectedType)
    val selectedStatsType = state.types.firstOrNull { it.type == selectedType }
    val accentColor = selectedStatsType?.accent?.color() ?: MaterialTheme.colorScheme.primary
    val layout = state.cardLayouts[statisticsLayoutTab(selectedType)] ?: StatisticsCardLayout()
    val sections = layout.visibleSections(isOverview, state.range)
    val listState = rememberLazyListState()
    // A saved layout change reorders the page; start from the top so the new order is visible.
    var shownLayout by remember(state.profileId, selectedType) { mutableStateOf(layout) }
    LaunchedEffect(layout) {
        if (layout != shownLayout) {
            shownLayout = layout
            listState.scrollToItem(0)
        }
    }
    val showToday = visibleActivity?.window?.let { it.range != StatsRange.ALL && !it.isLatest } == true

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = bottomPadding + 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "range") { StatisticsRangeSelector(state.range, onRangeSelected) }
        if (StatisticsCard.ACTIVITY in layout.hidden) {
            item(key = "period") {
                Column {
                    visibleActivity?.window?.let { window ->
                        StatisticsPeriodRow(
                            period = formatStatisticsWindow(window),
                            showToday = showToday,
                            onToday = onShowToday,
                        )
                    }
                    StatisticsActivityFeedback(state.activity, onRetryActivity)
                }
            }
        }
        if (state.incognito) {
            item(key = "incognito") {
                Text(
                    stringResource(MR.strings.statistics_incognito_active),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (sections.isEmpty() && layout.order.all { it in layout.hidden }) {
            item(key = "hidden") {
                Text(
                    stringResource(MR.strings.statistics_all_cards_hidden),
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        sections.forEach { (group, cards) ->
            if (group == StatisticsCardGroup.LIBRARY) {
                item(key = "library-header") {
                    StatisticsCurrentLibraryHeader(titleCount, Modifier.padding(top = 6.dp))
                }
            }
            cards.forEach { card ->
                item(key = card.id) {
                    when (card) {
                        StatisticsCard.SUMMARY -> StatisticsActivitySummaryCards(
                            activity = visibleActivity,
                            selectedType = selectedStatsType,
                            types = visibleTypes,
                            formatDuration = formatter,
                        )
                        StatisticsCard.ACTIVITY -> StatisticsActivityCard(
                            state = state.activity,
                            activity = visibleActivity,
                            types = visibleTypes,
                            formatter = formatter,
                            onNavigateByBuckets = onNavigateActivity,
                            onShowToday = onShowToday,
                            onShowLifetime = { onRangeSelected(StatsRange.ALL) },
                            onRetry = onRetryActivity,
                            onOpenActivity = { onOpenActivity(selectedType, it) },
                        )
                        StatisticsCard.CALENDAR -> state.calendar?.let { calendar ->
                            StatisticsReadingCalendarCard(
                                calendar = calendar,
                                type = selectedType,
                                color = accentColor,
                                trackingStartDate = activity?.trackingStartDate,
                                formatDuration = formatter,
                                onOpenDay = { day ->
                                    onOpenActivity(selectedType, StatsTrendPoint(day, day, emptyMap()))
                                },
                            )
                        }
                        StatisticsCard.TOP_TITLES -> if (visibleActivity?.topTitles?.isNotEmpty() == true) {
                            StatisticsTopTitlesCard(
                                titles = visibleActivity.topTitles,
                                totalDurationMillis = visibleActivity.totalDurationMillis,
                                typesById = state.types.associateBy(StatsType::type),
                                showType = isOverview,
                                formatDuration = formatter,
                                onTitleClick = onOpenEntry,
                                onSeeAll = { onOpenTopTitles(selectedType, visibleActivity) },
                            )
                        } else {
                            EmptyStatisticsCard(MR.strings.statistics_top_titles)
                        }
                        StatisticsCard.PATTERNS -> StatisticsActivityPatternsCard(
                            activity = visibleActivity,
                            color = accentColor,
                            formatDuration = formatter,
                        )
                        StatisticsCard.EARLIER -> visibleActivity?.let {
                            StatisticsLifetimeCard(
                                trackedDurationMillis = it.totalDurationMillis,
                                earlierDurationMillis = it.earlierDurationMillis,
                                trackingStartDate = it.trackingStartDate,
                                color = accentColor,
                                formatDuration = formatter,
                                onOpenEarlier = { onOpenEarlierActivity(selectedType) },
                            )
                        }
                        StatisticsCard.PROGRESS -> StatisticsProgressCard(progress)
                        StatisticsCard.MEDIA -> StatisticsLibraryCard(titleCounts, visibleTypes, onTypeSelected)
                        StatisticsCard.INSIGHTS -> state.library.insightsByType[selectedType]?.let {
                            StatisticsLibraryInsightsCard(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStatisticsCard(title: StringResource) {
    StatisticsSectionCard(title = stringResource(title)) { Text(stringResource(MR.strings.statistics_empty_period)) }
}
