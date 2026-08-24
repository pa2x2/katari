package eu.kanade.presentation.history.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityScreenModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HistoryActivityScreenContent(
    state: HistoryActivityScreenModel.State,
    startLocalDate: String,
    endLocalDate: String,
    type: EntryType?,
    paddingValues: PaddingValues,
    onEntryClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    when (state) {
        HistoryActivityScreenModel.State.Loading -> LoadingScreen(Modifier.padding(paddingValues))
        HistoryActivityScreenModel.State.Failed -> ActivityLoadFailed(paddingValues, onRetry)
        is HistoryActivityScreenModel.State.Success -> ActivitySessionList(
            state = state,
            startLocalDate = startLocalDate,
            endLocalDate = endLocalDate,
            type = type,
            paddingValues = paddingValues,
            onEntryClick = onEntryClick,
            onLoadMore = onLoadMore,
        )
    }
}

@Composable
private fun ActivityLoadFailed(paddingValues: PaddingValues, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(MR.strings.statistics_could_not_load_activity))
            TextButton(onClick = onRetry) { Text(stringResource(MR.strings.action_retry)) }
        }
    }
}

@Composable
private fun ActivitySessionList(
    state: HistoryActivityScreenModel.State.Success,
    startLocalDate: String,
    endLocalDate: String,
    type: EntryType?,
    paddingValues: PaddingValues,
    onEntryClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val startDate = remember(startLocalDate) { LocalDate.parse(startLocalDate) }
    val endDate = remember(endLocalDate) { LocalDate.parse(endLocalDate) }
    val rangeLabel = if (startDate == endDate) {
        startDate.format(dateFormatter)
    } else {
        "${startDate.format(dateFormatter)} – ${endDate.format(dateFormatter)}"
    }

    if (state.sessions.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(MR.strings.statistics_no_activity),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            top = paddingValues.calculateTopPadding() + 12.dp,
            end = 16.dp,
            bottom = paddingValues.calculateBottomPadding() + 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (startDate != endDate || type != null) {
            item("range") {
                Column {
                    if (startDate != endDate) {
                        Text(rangeLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (type != null) {
                        Text(
                            text = stringResource(type.entryTypePresentation().displayNameLabel),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        itemsIndexed(
            items = state.sessions,
            key = { _, session -> session.sessionId },
        ) { index, session ->
            if (index == 0 || state.sessions[index - 1].localDate != session.localDate) {
                Text(
                    text = LocalDate.parse(session.localDate).format(dateFormatter),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
            ActivitySessionCard(session = session, showType = type == null, onClick = onEntryClick)
        }
        if (state.hasMore || state.loadMoreFailed) {
            item("load-more") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (state.loadingMore) {
                        CircularProgressIndicator()
                    } else {
                        TextButton(onClick = onLoadMore) {
                            Text(
                                stringResource(
                                    if (state.loadMoreFailed) {
                                        MR.strings.action_retry
                                    } else {
                                        MR.strings.statistics_show_more_activity
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
