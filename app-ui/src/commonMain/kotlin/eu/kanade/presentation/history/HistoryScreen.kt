package eu.kanade.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.DeleteSweep
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.feedback.EmptyScreen
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.feedback.NoResults
import app.katari.presentation.kit.feedback.SnackbarHostState
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.AppBarActions
import app.katari.presentation.kit.navigation.AppBarTitle
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.presentation.kit.navigation.SearchToolbar
import app.katari.presentation.kit.selection.SelectableItems
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariPreviewTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.profiles.ui.switcher.ActiveProfileButton
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.history.components.HistoryListItem
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.util.rememberTimeFormatter
import eu.kanade.tachiyomi.ui.history.HistoryViewModel
import kotlinx.datetime.LocalDate
import tachiyomi.domain.history.model.HistoryItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun HistoryScreen(
    state: HistoryViewModel.State,
    dayDurations: Map<LocalDate, Long>,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String?) -> Unit,
    onClickDay: (LocalDate) -> Unit,
    onClickCover: (HistoryUiItem) -> Unit,
    canResume: (HistoryUiItem) -> Boolean,
    onClickResume: (HistoryUiItem) -> Unit,
    onClickDelete: (HistoryItem) -> Unit,
    onDialogChange: (HistoryViewModel.Dialog?) -> Unit,
    syncAction: AppBar.Action?,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            SearchToolbar(
                titleContent = { AppBarTitle(stringResource(MR.strings.history)) },
                searchQuery = state.searchQuery,
                onChangeSearchQuery = onSearchQueryChange,
                actions = {
                    AppBarActions(
                        listOfNotNull(
                            syncAction,
                            AppBar.Action(
                                title = stringResource(MR.strings.pref_clear_history),
                                icon = MaterialSymbols.Rounded.DeleteSweep,
                                onClick = {
                                    onDialogChange(HistoryViewModel.Dialog.DeleteAll)
                                },
                            ),
                        ),
                    )
                    ActiveProfileButton()
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHostState = snackbarHostState,
    ) { contentPadding ->
        state.list.let {
            if (it == null) {
                LoadingScreen(Modifier.padding(contentPadding))
            } else if (it.isEmpty()) {
                if (!state.searchQuery.isNullOrEmpty()) {
                    NoResults(modifier = Modifier.padding(contentPadding))
                } else {
                    EmptyScreen(
                        stringRes = MR.strings.information_no_recent_history,
                        modifier = Modifier.padding(contentPadding),
                    )
                }
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
) {
    val formatDuration = rememberStatisticsDurationFormatter()
    val timeFormatter = rememberTimeFormatter()
    val positions = remember(history) { RowGroupPosition.ofRuns(history) { it is HistoryUiModel.Item } }
    // History has no selection, so a right-click only opens an entry's own menu
    SelectableItems(selection = null) {
        RowList(
            fastScroll = true,
            contentPadding = contentPadding,
        ) {
            itemsIndexed(
                items = history,
                key = { _, item -> "history-${item.hashCode()}" },
                contentType = { _, item ->
                    when (item) {
                        is HistoryUiModel.Header -> "header"
                        is HistoryUiModel.Item -> "item"
                    }
                },
            ) { index, item ->
                when (item) {
                    is HistoryUiModel.Header -> {
                        val duration = dayDurations[item.date]?.takeIf { it > 0L }
                        RowGroupHeader(
                            text = relativeDateText(item.date),
                            modifier = Modifier
                                .animateItem()
                                .clickable(enabled = duration != null) { onClickDay(item.date) },
                            trailing = duration?.let {
                                { Text(formatDuration(it), role = TextRole.Body, tone = ContentTone.Secondary) }
                            },
                        )
                    }
                    is HistoryUiModel.Item -> RowGroupItem(
                        position = positions[index] ?: RowGroupPosition.Alone,
                        modifier = Modifier.animateItem(),
                    ) {
                        HistoryListItem(
                            item = item.item,
                            timeFormatter = timeFormatter,
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
}

sealed class HistoryUiModel {
    data class Header(val date: LocalDate) : HistoryUiModel()
    data class Item(val item: HistoryUiItem) : HistoryUiModel()
}

@PreviewLightDark
@Composable
internal fun HistoryScreenPreviews(
    @PreviewParameter(HistoryViewModelStateProvider::class)
    historyState: HistoryViewModel.State,
) {
    KatariPreviewTheme {
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
            onDialogChange = {},
            syncAction = null,
        )
    }
}
