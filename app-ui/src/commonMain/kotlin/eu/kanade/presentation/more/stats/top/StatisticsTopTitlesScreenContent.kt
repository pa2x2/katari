package eu.kanade.presentation.more.stats.top

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import eu.kanade.presentation.more.stats.components.StatisticsTopTitleRow
import eu.kanade.presentation.more.stats.components.color
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.ui.stats.top.StatisticsTopTitlesViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

@Composable
fun StatisticsTopTitlesScreenContent(
    state: StatisticsTopTitlesViewModel.State,
    types: List<StatsType>,
    totalDurationMillis: Long,
    paddingValues: PaddingValues,
    onLoadMore: () -> Unit,
    onEntryClick: (Long) -> Unit,
) {
    if (state.titles.isEmpty()) {
        when {
            state.failed -> EmptyScreen(
                stringRes = MR.strings.statistics_could_not_load_activity,
                modifier = Modifier.padding(paddingValues),
                actions = listOf(
                    EmptyScreenAction(
                        stringRes = MR.strings.action_retry,
                        icon = MaterialSymbols.Rounded.Refresh,
                        onClick = onLoadMore,
                    ),
                ),
            )
            state.loading || !state.endReached -> LoadingScreen(Modifier.padding(paddingValues))
            else -> EmptyScreen(MR.strings.statistics_empty_period, Modifier.padding(paddingValues))
        }
        return
    }
    val formatDuration = rememberStatisticsDurationFormatter()
    val typesById = types.associateBy(StatsType::type)
    val listState = rememberLazyListState()
    // Re-evaluated after every page, so a first page shorter than the screen still asks for more.
    LaunchedEffect(listState, state.titles.size) {
        snapshotFlow { listState.layoutInfo.run { (visibleItemsInfo.lastOrNull()?.index ?: 0) >= totalItemsCount - 5 } }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
    RowList(
        state = listState,
        contentPadding = paddingValues + PaddingValues(top = KatariTheme.spacing.xs, bottom = KatariTheme.spacing.xl),
    ) {
        itemsIndexed(state.titles, key = { _, entry -> entry.entryId }) { index, entry ->
            val type = typesById[entry.type]
            RowGroupItem(position = RowGroupPosition.of(index, state.titles.size)) {
                StatisticsTopTitleRow(
                    rank = index + 1,
                    title = entry.title,
                    cover = entry.cover,
                    type = entry.type,
                    completionCount = entry.completionCount,
                    durationMillis = entry.durationMillis,
                    shareOf = totalDurationMillis,
                    color = type?.accent?.color() ?: KatariTheme.chart.series,
                    formatDuration = formatDuration,
                    onClick = { onEntryClick(entry.entryId) },
                    inRowGroup = true,
                )
            }
        }
        when {
            state.failed -> item(key = "failed") {
                NoticeCard(
                    text = stringResource(MR.strings.statistics_could_not_load_activity),
                    tone = NoticeTone.Warning,
                    actionLabel = stringResource(MR.strings.action_retry),
                    onAction = onLoadMore,
                )
            }
            state.loading -> item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(KatariTheme.spacing.l), contentAlignment = Alignment.Center) {
                    LoadingIndicator(Modifier.size(KatariTheme.sizes.icon))
                }
            }
        }
    }
}
