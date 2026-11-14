package eu.kanade.presentation.history.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Refresh
import app.katari.presentation.kit.feedback.EmptyScreen
import app.katari.presentation.kit.feedback.EmptyScreenAction
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.progress.LoadingIndicator
import app.katari.presentation.kit.surface.NoticeCard
import app.katari.presentation.kit.surface.NoticeTone
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.presentation.more.stats.components.color
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivitySummary
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

/**
 * @param oneTitle the list is one title's: each day lists its chapters or episodes, and opens up into its sessions,
 * instead of listing titles.
 */
@Composable
fun HistoryActivityScreenContent(
    state: HistoryActivityViewModel.State,
    summary: HistoryActivitySummary?,
    types: List<StatsType>,
    oneTitle: Boolean,
    paddingValues: PaddingValues,
    onEntryClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    when (state) {
        HistoryActivityViewModel.State.Loading -> LoadingScreen(Modifier.padding(paddingValues))
        HistoryActivityViewModel.State.Failed -> ActivityLoadFailed(paddingValues, onRetry)
        is HistoryActivityViewModel.State.Success -> if (state.days.isEmpty()) {
            EmptyScreen(stringRes = MR.strings.statistics_no_activity, modifier = Modifier.padding(paddingValues))
        } else {
            ActivityDayList(state, summary, types, oneTitle, paddingValues, onEntryClick, onLoadMore)
        }
    }
}

@Composable
private fun ActivityLoadFailed(paddingValues: PaddingValues, onRetry: () -> Unit) {
    EmptyScreen(
        stringRes = MR.strings.statistics_could_not_load_activity,
        modifier = Modifier.padding(paddingValues),
        actions = listOf(
            EmptyScreenAction(
                stringRes = MR.strings.action_retry,
                icon = MaterialSymbols.Rounded.Refresh,
                onClick = onRetry,
            ),
        ),
    )
}

@Composable
private fun ActivityDayList(
    state: HistoryActivityViewModel.State.Success,
    summary: HistoryActivitySummary?,
    types: List<StatsType>,
    oneTitle: Boolean,
    paddingValues: PaddingValues,
    onEntryClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
) {
    val formatDuration = rememberStatisticsDurationFormatter()
    val colors = types.associate { it.type to it.accent.color() }
    // Days opened up into their sessions (one title's list) and titles unfolded into their parts (a period's list).
    var openDays by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var unfoldedTitles by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val listState = rememberLazyListState()
    // Re-evaluated after every page, so a first page shorter than the screen still asks for more.
    LaunchedEffect(listState, state.days.size, state.hasMore, state.loadMoreFailed) {
        if (!state.hasMore || state.loadMoreFailed) return@LaunchedEffect
        snapshotFlow {
            listState.layoutInfo.run {
                (visibleItemsInfo.lastOrNull()?.index ?: 0) >= totalItemsCount - LOAD_AHEAD_ITEMS
            }
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
    RowList(
        state = listState,
        contentPadding = paddingValues + PaddingValues(top = KatariTheme.spacing.m, bottom = KatariTheme.spacing.xl),
    ) {
        // Always present so the list stays anchored at the top when the summary arrives after the first page.
        item("summary") {
            summary?.let { HistoryActivitySummaryCard(it, types, colors) }
        }
        state.days.forEach { day ->
            val date = day.localDate.toString()
            item("header-$date") {
                ActivityDayHeader(
                    day = day,
                    colors = colors,
                    formatDuration = formatDuration,
                    showsSessions = (date in openDays).takeIf { oneTitle },
                    onToggleSessions = { openDays = openDays.toggle(date) },
                )
            }
            item("day-$date") {
                RowGroupItem(position = RowGroupPosition.Alone) {
                    if (oneTitle) {
                        ActivityDayParts(day, showsSessions = date in openDays, formatDuration = formatDuration)
                    } else {
                        ActivityDayTitles(
                            day = day,
                            unfoldedTitles = unfoldedTitles,
                            formatDuration = formatDuration,
                            onEntryClick = onEntryClick,
                            onToggleUnfolded = { unfoldedTitles = unfoldedTitles.toggle(it) },
                        )
                    }
                }
            }
        }
        when {
            state.loadMoreFailed -> item("load-more-failed") {
                NoticeCard(
                    text = stringResource(MR.strings.statistics_could_not_load_activity),
                    tone = NoticeTone.Warning,
                    actionLabel = stringResource(MR.strings.action_retry),
                    onAction = onLoadMore,
                )
            }
            state.hasMore -> item("loading-more") {
                Box(Modifier.fillMaxWidth().padding(KatariTheme.spacing.l), contentAlignment = Alignment.Center) {
                    LoadingIndicator(Modifier.size(KatariTheme.sizes.icon))
                }
            }
        }
    }
}

private fun Set<String>.toggle(key: String): Set<String> = if (key in this) this - key else this + key

private const val LOAD_AHEAD_ITEMS = 6
