package eu.kanade.presentation.more.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.stats.components.StatisticsActivityCard
import eu.kanade.presentation.more.stats.components.StatisticsActivityFeedback
import eu.kanade.presentation.more.stats.components.StatisticsActivityPatternsCard
import eu.kanade.presentation.more.stats.components.StatisticsActivitySummaryCards
import eu.kanade.presentation.more.stats.components.StatisticsLibraryInsightsCard
import eu.kanade.presentation.more.stats.components.StatisticsLifetimeCard
import eu.kanade.presentation.more.stats.components.StatisticsMediaCard
import eu.kanade.presentation.more.stats.components.StatisticsPeriodRow
import eu.kanade.presentation.more.stats.components.StatisticsProgressCard
import eu.kanade.presentation.more.stats.components.StatisticsReadingCalendarCard
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
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.surface.DashboardCard
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.statistics.model.StatisticsCard
import tachiyomi.domain.statistics.model.StatisticsCardLayout
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun StatisticsDashboardPage(
    state: StatsScreenState.Success,
    selectedType: EntryType?,
    bottomPadding: Dp,
    onRangeSelected: (StatsRange) -> Unit,
    onTypeSelected: (EntryType?) -> Unit,
    onNavigateActivity: (Int) -> Unit,
    onPageActivity: (Int) -> Unit,
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
    val accentColor = selectedStatsType?.accent?.color() ?: KatariTheme.chart.series
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

    RowList(
        state = listState,
        contentPadding = PaddingValues(bottom = bottomPadding + KatariTheme.spacing.xl),
    ) {
        item(key = "activity-header") { DashboardGroupHeader(stringResource(StatisticsCardGroup.ACTIVITY.label)) }
        item(key = "range") {
            DashboardItem { StatisticsRangeSelector(state.range, onRangeSelected) }
        }
        if (StatisticsCard.ACTIVITY in layout.hidden) {
            item(key = "period") {
                DashboardItem {
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
                DashboardItem {
                    Text(stringResource(MR.strings.statistics_incognito_active), tone = ContentTone.Secondary)
                }
            }
        }
        if (sections.isEmpty() && layout.order.all { it in layout.hidden }) {
            item(key = "hidden") {
                DashboardItem {
                    Text(
                        stringResource(MR.strings.statistics_all_cards_hidden),
                        modifier = Modifier.padding(vertical = KatariTheme.spacing.xl),
                    )
                }
            }
        }
        sections.forEach { (group, cards) ->
            if (group == StatisticsCardGroup.LIBRARY) {
                item(key = "library-header") {
                    DashboardGroupHeader(
                        text = stringResource(group.label),
                        count = pluralStringResource(MR.plurals.statistics_title_count, titleCount, titleCount),
                    )
                }
            }
            cards.forEach { card ->
                item(key = card.id) {
                    DashboardCardItem {
                        when (card) {
                            StatisticsCard.SUMMARY -> StatisticsActivitySummaryCards(
                                activity = visibleActivity,
                                selectedType = selectedStatsType,
                                types = visibleTypes,
                                goal = state.goal,
                                formatDuration = formatter,
                                // Tiles keep to the cards' side margin.
                                modifier = Modifier.padding(horizontal = KatariTheme.spacing.m),
                            )
                            StatisticsCard.ACTIVITY -> StatisticsActivityCard(
                                state = state.activity,
                                activity = visibleActivity,
                                types = visibleTypes,
                                formatter = formatter,
                                onNavigateByBuckets = onNavigateActivity,
                                onNavigateByWindows = onPageActivity,
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
                            StatisticsCard.PROGRESS -> StatisticsProgressCard(progress, accentColor)
                            StatisticsCard.MEDIA -> StatisticsMediaCard(
                                titleCounts = titleCounts,
                                progressByType = state.library.progressByType,
                                types = visibleTypes,
                                onTypeClick = onTypeSelected,
                            )
                            StatisticsCard.INSIGHTS -> selectedType?.let { type ->
                                state.library.insightsByType[type]?.let { insights ->
                                    StatisticsLibraryInsightsCard(insights, type, titleCount, accentColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The header of the activity or library half of the dashboard, with the library's [count] of titles after it. */
@Composable
private fun DashboardGroupHeader(text: String, count: String? = null) {
    RowGroupHeader(
        text = text,
        modifier = Modifier.semantics { heading() },
        trailing = count?.let { { Pill(text = it) } },
    )
}

// Rows that aren't cards keep to the cards' side margin, which is the one row group cards keep.
@Composable
private fun DashboardItem(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = KatariTheme.spacing.m)) {
        content()
        RowGroupGap()
    }
}

@Composable
private fun DashboardCardItem(content: @Composable () -> Unit) {
    Column {
        content()
        RowGroupGap()
    }
}

@Composable
private fun EmptyStatisticsCard(title: StringResource) {
    DashboardCard(title = stringResource(title)) { Text(stringResource(MR.strings.statistics_empty_period)) }
}
