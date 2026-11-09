package eu.kanade.presentation.history.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Refresh
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.feedback.EmptyScreen
import app.katari.presentation.kit.feedback.EmptyScreenAction
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.progress.LoadingIndicator
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivitySummary
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityViewModel
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HistoryActivityScreenContent(
    state: HistoryActivityViewModel.State,
    summary: HistoryActivitySummary?,
    type: EntryType?,
    types: List<StatsType>,
    showSessionType: Boolean,
    showSessionEntry: Boolean,
    paddingValues: PaddingValues,
    onEntryClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    when (state) {
        HistoryActivityViewModel.State.Loading -> LoadingScreen(Modifier.padding(paddingValues))
        HistoryActivityViewModel.State.Failed -> ActivityLoadFailed(paddingValues, onRetry)
        is HistoryActivityViewModel.State.Success -> ActivitySessionList(
            state = state,
            summary = summary,
            type = type,
            types = types,
            showSessionType = showSessionType,
            showSessionEntry = showSessionEntry,
            paddingValues = paddingValues,
            onEntryClick = onEntryClick,
            onLoadMore = onLoadMore,
        )
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
private fun ActivitySessionList(
    state: HistoryActivityViewModel.State.Success,
    summary: HistoryActivitySummary?,
    type: EntryType?,
    types: List<StatsType>,
    showSessionType: Boolean,
    showSessionEntry: Boolean,
    paddingValues: PaddingValues,
    onEntryClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    if (state.sessions.isEmpty()) {
        EmptyScreen(
            stringRes = MR.strings.statistics_no_activity,
            modifier = Modifier.padding(paddingValues),
        )
        return
    }

    val sessions = state.sessions
    RowList(
        contentPadding = paddingValues + PaddingValues(top = KatariTheme.spacing.m, bottom = KatariTheme.spacing.xl),
    ) {
        // Always present so the list stays anchored at the top when the summary arrives after the first page.
        item("summary") {
            summary?.let { HistoryActivitySummaryCard(it, type, types) }
        }
        itemsIndexed(
            items = sessions,
            key = { _, session -> session.sessionId },
        ) { index, session ->
            val startsDay = index == 0 || sessions[index - 1].localDate != session.localDate
            val endsDay = index == sessions.lastIndex || sessions[index + 1].localDate != session.localDate
            Column {
                if (startsDay) {
                    RowGroupHeader(LocalDate.parse(session.localDate).format(dateFormatter))
                }
                RowGroupItem(
                    position = when {
                        startsDay && endsDay -> RowGroupPosition.Alone
                        startsDay -> RowGroupPosition.First
                        endsDay -> RowGroupPosition.Last
                        else -> RowGroupPosition.Middle
                    },
                ) {
                    ActivitySession(
                        session = session,
                        showType = showSessionType,
                        showEntry = showSessionEntry,
                        onClick = onEntryClick,
                    )
                }
            }
        }
        if (state.hasMore || state.loadMoreFailed) {
            item("load-more") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (state.loadingMore) {
                        LoadingIndicator()
                    } else {
                        Button(
                            label = stringResource(
                                if (state.loadMoreFailed) {
                                    MR.strings.action_retry
                                } else {
                                    MR.strings.statistics_show_more_activity
                                },
                            ),
                            onClick = onLoadMore,
                            style = ButtonStyle.Text,
                        )
                    }
                }
            }
        }
    }
}
