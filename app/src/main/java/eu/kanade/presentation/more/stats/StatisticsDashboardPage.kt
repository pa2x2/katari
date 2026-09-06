package eu.kanade.presentation.more.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.stats.components.StatisticsActivityCard
import eu.kanade.presentation.more.stats.components.StatisticsActivityFeedback
import eu.kanade.presentation.more.stats.components.StatisticsActivityHeader
import eu.kanade.presentation.more.stats.components.StatisticsActivityPatternsCard
import eu.kanade.presentation.more.stats.components.StatisticsActivitySummaryCards
import eu.kanade.presentation.more.stats.components.StatisticsEarlierActivityCard
import eu.kanade.presentation.more.stats.components.StatisticsLibraryCard
import eu.kanade.presentation.more.stats.components.StatisticsLibraryInsightsCard
import eu.kanade.presentation.more.stats.components.StatisticsProgressCard
import eu.kanade.presentation.more.stats.components.StatisticsSectionCard
import eu.kanade.presentation.more.stats.components.StatisticsTopTitlesCard
import eu.kanade.presentation.more.stats.components.color
import eu.kanade.presentation.more.stats.components.formatStatisticsWindow
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.presentation.more.stats.data.forType
import eu.kanade.presentation.more.stats.layout.StatisticsLayoutEditor
import eu.kanade.presentation.more.stats.layout.isCurrentLibrary
import eu.kanade.presentation.more.stats.layout.statisticsCards
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.statistics.model.StatisticsCard
import tachiyomi.domain.statistics.model.StatisticsCardLayout
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat

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
    onOpenEarlierActivity: (EntryType?, Long?) -> Unit,
    onSaveLayout: (Long, String, StatisticsCardLayout) -> Unit,
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
    val secondaryMetricValue = if (selectedStatsType == null) {
        streakDays?.let { pluralStringResource(MR.plurals.day, it, it) } ?: "—"
    } else {
        visibleActivity?.completionCount?.let(NumberFormat.getIntegerInstance()::format) ?: "—"
    }
    val secondaryMetricLabel = selectedStatsType?.let { stringResource(it.consumedUnitLabel) }
        ?: stringResource(
            if (visibleActivity?.window?.isLatest == false) {
                MR.strings.statistics_ending_streak
            } else {
                MR.strings.statistics_current_streak
            },
        )
    val tab = selectedType?.name ?: "overview"
    val layout = state.cardLayouts[tab] ?: StatisticsCardLayout()
    var editing by remember(state.profileId, tab) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var scrollAfterSave by remember(state.profileId, tab) { mutableStateOf(false) }
    LaunchedEffect(layout) {
        if (scrollAfterSave) {
            listState.scrollToItem(0)
            scrollAfterSave = false
        }
    }
    val period = visibleActivity?.window?.let(::formatStatisticsWindow).orEmpty()
    val cards = statisticsCards(selectedType == null)
    if (editing) {
        StatisticsLayoutEditor(layout, selectedType == null, onDismiss = { editing = false }, onSave = {
            scrollAfterSave = it != layout
            onSaveLayout(state.profileId, tab, it)
            editing = false
        })
    }
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                StatisticsActivityHeader(
                    showToday = visibleActivity?.window?.let { it.range != StatsRange.ALL && !it.isLatest } == true,
                    onToday = onShowToday,
                )
            }
            TextButton(onClick = { editing = true }) { Text(stringResource(MR.strings.statistics_customize)) }
        }
        Box(Modifier.padding(horizontal = 16.dp)) { StatisticsRangeSelector(state.range, onRangeSelected) }
        if (StatisticsCard.ACTIVITY in layout.hidden) {
            if (period.isNotEmpty()) {
                Text(
                    text = period,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatisticsActivityFeedback(state.activity, onRetryActivity)
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = bottomPadding + 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.incognito) {
                item(key = "incognito") {
                    Text(
                        stringResource(MR.strings.statistics_incognito_active),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (layout.order.none { it in cards && it !in layout.hidden }) {
                item(key = "hidden") {
                    Text(
                        stringResource(MR.strings.statistics_all_cards_hidden),
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            layout.order.filter { it in cards && it !in layout.hidden }.forEach { card ->
                item(key = card.id) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (card.isCurrentLibrary) {
                            Text(
                                text = stringResource(MR.strings.statistics_library) + " · " +
                                    pluralStringResource(
                                        MR.plurals.statistics_title_count,
                                        titleCount,
                                        titleCount,
                                    ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        when (card) {
                            StatisticsCard.SUMMARY -> StatisticsActivitySummaryCards(
                                time = visibleActivity?.let { formatter(it.totalDurationMillis) } ?: "—",
                                secondaryValue = secondaryMetricValue,
                                secondaryLabel = secondaryMetricLabel,
                            )
                            StatisticsCard.ACTIVITY -> StatisticsActivityCard(
                                state = state.activity,
                                activity = visibleActivity,
                                types = visibleTypes,
                                formatter = formatter,

                                onNavigateByBuckets = onNavigateActivity,
                                onRetry = onRetryActivity,
                                onOpenActivity = { onOpenActivity(selectedType, it) },
                            )
                            StatisticsCard.TOP_TITLES -> if (visibleActivity?.topTitles?.isNotEmpty() == true) {
                                StatisticsTopTitlesCard(
                                    visibleActivity.topTitles,
                                    state.types.associateBy(StatsType::type),
                                    formatter,
                                    onOpenEntry,
                                )
                            } else {
                                EmptyStatisticsCard(MR.strings.statistics_top_titles)
                            }
                            StatisticsCard.PATTERNS -> StatisticsActivityPatternsCard(
                                visibleActivity?.sessionCount,
                                visibleActivity?.averageSessionDurationMillis,
                                visibleActivity?.longestSessionDurationMillis,
                                visibleActivity?.activeDays,
                                formatter,
                            )
                            StatisticsCard.EARLIER -> if (visibleActivity != null &&
                                visibleActivity.earlierDurationMillis > 0L
                            ) {
                                StatisticsEarlierActivityCard(
                                    duration = formatter(visibleActivity.earlierDurationMillis),
                                    beforeDate = null,
                                    onClick = {
                                        onOpenEarlierActivity(
                                            selectedType,
                                            visibleActivity.trackingStartedAtEpochMillis,
                                        )
                                    },

                                )
                            } else {
                                EmptyStatisticsCard(MR.strings.statistics_earlier_activity)
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
}

@Composable
private fun EmptyStatisticsCard(title: StringResource) {
    StatisticsSectionCard(title = stringResource(title)) { Text(stringResource(MR.strings.statistics_empty_period)) }
}
