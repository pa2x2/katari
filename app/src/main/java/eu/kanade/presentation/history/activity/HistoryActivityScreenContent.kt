package eu.kanade.presentation.history.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivitySummary
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityViewModel
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.EmptyScreenAction
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
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

    LazyColumn(
        contentPadding = PaddingValues(
            start = KatariTheme.spacing.contentInset,
            top = paddingValues.calculateTopPadding() + KatariTheme.spacing.m,
            end = KatariTheme.spacing.contentInset,
            bottom = paddingValues.calculateBottomPadding() + KatariTheme.spacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
    ) {
        // Always present so the list stays anchored at the top when the summary arrives after the first page.
        item("summary") { summary?.let { HistoryActivitySummaryCard(it, type, types) } }
        itemsIndexed(
            items = state.sessions,
            key = { _, session -> session.sessionId },
        ) { index, session ->
            if (index == 0 || state.sessions[index - 1].localDate != session.localDate) {
                Text(
                    text = LocalDate.parse(session.localDate).format(dateFormatter),
                    modifier = Modifier.padding(top = KatariTheme.spacing.s),
                    role = TextRole.GroupLabel,
                    tone = ContentTone.Secondary,
                )
                Spacer(Modifier.height(KatariTheme.spacing.s))
            }
            ActivitySessionCard(
                session = session,
                showType = showSessionType,
                showEntry = showSessionEntry,
                onClick = onEntryClick,
            )
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
