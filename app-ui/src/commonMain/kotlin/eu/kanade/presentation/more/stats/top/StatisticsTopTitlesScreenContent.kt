package eu.kanade.presentation.more.stats.top

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.feedback.EmptyScreen
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.layout.FramedContent
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.progress.LoadingIndicator
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.presentation.more.stats.components.StatisticsTopTitleRow
import eu.kanade.presentation.more.stats.components.color
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.top.StatisticsTopTitlesViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

@Composable
fun StatisticsTopTitlesScreenContent(
    state: StatisticsTopTitlesViewModel.State,
    selectedType: EntryType?,
    types: List<StatsType>,
    totalDurationMillis: Long,
    paddingValues: PaddingValues,
    onLoadMore: () -> Unit,
    onEntryClick: (Long) -> Unit,
) {
    if (state.titles.isEmpty()) {
        when {
            state.failed -> LoadFailed(onLoadMore, Modifier.fillMaxSize().padding(paddingValues))
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
                    typeLabel = type?.takeIf { selectedType == null }?.let { stringResource(it.displayName) },
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
            state.failed -> item(key = "failed") { LoadFailed(onLoadMore, Modifier.fillMaxWidth()) }
            state.loading -> item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(KatariTheme.spacing.l), contentAlignment = Alignment.Center) {
                    LoadingIndicator(Modifier.size(KatariTheme.sizes.icon))
                }
            }
        }
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit, modifier: Modifier) {
    FramedContent(modifier.padding(KatariTheme.spacing.l), contentAlignment = Alignment.Center) {
        Button(
            label = stringResource(MR.strings.statistics_could_not_load_activity) + " · " +
                stringResource(MR.strings.action_retry),
            onClick = onRetry,
            style = ButtonStyle.Text,
        )
    }
}
