package eu.kanade.presentation.more.stats.top

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.StatisticsTopTitleRow
import eu.kanade.presentation.more.stats.components.color
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.top.StatisticsTopTitlesScreenModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun StatisticsTopTitlesScreenContent(
    state: StatisticsTopTitlesScreenModel.State,
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
            else -> Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(MR.strings.statistics_empty_period),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = paddingValues.calculateTopPadding() + 4.dp,
            end = 16.dp,
            bottom = paddingValues.calculateBottomPadding() + 20.dp,
        ),
    ) {
        itemsIndexed(state.titles, key = { _, entry -> entry.entryId }) { index, entry ->
            val type = typesById[entry.type]
            StatisticsTopTitleRow(
                rank = index + 1,
                title = entry.title,
                cover = entry.cover,
                type = entry.type,
                typeLabel = type?.takeIf { selectedType == null }?.let { stringResource(it.displayName) },
                completionCount = entry.completionCount,
                durationMillis = entry.durationMillis,
                shareOf = totalDurationMillis,
                color = type?.accent?.color() ?: MaterialTheme.colorScheme.primary,
                formatDuration = formatDuration,
                onClick = { onEntryClick(entry.entryId) },
            )
        }
        when {
            state.failed -> item(key = "failed") { LoadFailed(onLoadMore, Modifier.fillMaxWidth()) }
            state.loading -> item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit, modifier: Modifier) {
    Box(modifier.padding(16.dp), contentAlignment = Alignment.Center) {
        TextButton(onClick = onRetry) {
            Text(
                stringResource(MR.strings.statistics_could_not_load_activity) + " · " +
                    stringResource(MR.strings.action_retry),
            )
        }
    }
}
