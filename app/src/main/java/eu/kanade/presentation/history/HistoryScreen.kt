package eu.kanade.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.AppSnackbarHost
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.history.components.HistoryListItem
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import kotlinx.datetime.LocalDate
import tachiyomi.domain.history.model.HistoryItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun HistoryScreen(
    state: HistoryScreenModel.State,
    dayDurations: Map<LocalDate, Long>,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String?) -> Unit,
    onClickDay: (LocalDate) -> Unit,
    onClickCover: (HistoryUiItem) -> Unit,
    canResume: (HistoryUiItem) -> Boolean,
    onClickResume: (HistoryUiItem) -> Unit,
    onClickDelete: (HistoryItem) -> Unit,
    onClickFavorite: (HistoryUiItem) -> Unit,
    onDialogChange: (HistoryScreenModel.Dialog?) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            SearchToolbar(
                titleContent = { AppBarTitle(stringResource(MR.strings.history)) },
                searchQuery = state.searchQuery,
                onChangeSearchQuery = onSearchQueryChange,
                actions = {
                    AppBarActions(
                        listOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.pref_clear_history),
                                icon = Icons.Outlined.DeleteSweep,
                                onClick = {
                                    onDialogChange(HistoryScreenModel.Dialog.DeleteAll)
                                },
                            ),
                        ),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        state.list.let {
            if (it == null) {
                LoadingScreen(Modifier.padding(contentPadding))
            } else if (it.isEmpty()) {
                val msg = if (!state.searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.information_no_recent_history
                }
                EmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                HistoryScreenContent(
                    history = it,
                    dayDurations = dayDurations,
                    contentPadding = contentPadding,
                    onClickDay = onClickDay,
                    onClickCover = onClickCover,
                    canResume = canResume,
                    onClickResume = onClickResume,
                    onClickDelete = onClickDelete,
                    onClickFavorite = onClickFavorite,
                )
            }
        }
    }
}

@Composable
private fun HistoryScreenContent(
    history: List<HistoryUiModel>,
    dayDurations: Map<LocalDate, Long>,
    contentPadding: PaddingValues,
    onClickDay: (LocalDate) -> Unit,
    onClickCover: (HistoryUiItem) -> Unit,
    canResume: (HistoryUiItem) -> Boolean,
    onClickResume: (HistoryUiItem) -> Unit,
    onClickDelete: (HistoryItem) -> Unit,
    onClickFavorite: (HistoryUiItem) -> Unit,
) {
    val formatDuration = rememberStatisticsDurationFormatter()
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = history,
            key = { "history-${it.hashCode()}" },
            contentType = {
                when (it) {
                    is HistoryUiModel.Header -> "header"
                    is HistoryUiModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is HistoryUiModel.Header -> {
                    val duration = dayDurations[item.date]?.takeIf { it > 0L }
                    ListGroupHeader(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .clickable(enabled = duration != null) { onClickDay(item.date) },
                        text = listOfNotNull(relativeDateText(item.date), duration?.let(formatDuration))
                            .joinToString(" · "),
                    )
                }
                is HistoryUiModel.Item -> {
                    HistoryListItem(
                        modifier = Modifier.animateItem(),
                        item = item.item,
                        onClickCover = { onClickCover(item.item) },
                        onClickResume = if (canResume(item.item)) {
                            { onClickResume(item.item) }
                        } else {
                            null
                        },
                        onClickDelete = { onClickDelete(item.item.historyItem) },
                    )
                }
            }
        }
    }
}

sealed class HistoryUiModel {
    data class Header(val date: LocalDate) : HistoryUiModel()
    data class Item(val item: HistoryUiItem) : HistoryUiModel()
}

@PreviewLightDark
@Composable
internal fun HistoryScreenPreviews(
    @PreviewParameter(HistoryScreenModelStateProvider::class)
    historyState: HistoryScreenModel.State,
) {
    TachiyomiPreviewTheme {
        HistoryScreen(
            state = historyState,
            dayDurations = emptyMap(),
            snackbarHostState = SnackbarHostState(),
            onSearchQueryChange = {},
            onClickDay = {},
            onClickCover = {},
            canResume = { true },
            onClickResume = {},
            onClickDelete = {},
            onClickFavorite = {},
            onDialogChange = {},
        )
    }
}
