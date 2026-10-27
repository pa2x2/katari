package eu.kanade.tachiyomi.ui.entry.related

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.presentation.util.formattedMessage
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.EmptyScreenAction
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.progress.PullRefresh
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun RelatedEntriesDialog(
    viewModel: RelatedEntriesViewModel,
    onDismissRequest: () -> Unit,
    onEntryClick: (Entry) -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    // A sheet capped below the window's height; the grid fills it, so loading and empty states keep its size.
    Sheet(onDismissRequest = onDismissRequest, capHeight = true) {
        Column(modifier = Modifier.fillMaxHeight()) {
            SheetHeader(title = stringResource(MR.strings.related_entries))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (val currentState = state) {
                    RelatedEntriesViewModel.State.Idle,
                    RelatedEntriesViewModel.State.Loading,
                    -> LoadingScreen(Modifier.fillMaxSize())

                    is RelatedEntriesViewModel.State.Error -> EmptyScreen(
                        message = with(context) { currentState.throwable.formattedMessage },
                        modifier = Modifier.fillMaxSize(),
                        actions = listOf(
                            EmptyScreenAction(
                                stringRes = MR.strings.action_retry,
                                icon = MaterialSymbols.Rounded.Refresh,
                                onClick = viewModel::retry,
                            ),
                        ),
                    )

                    is RelatedEntriesViewModel.State.Unavailable -> EmptyScreen(
                        message = stringResource(MR.strings.no_results_found),
                        modifier = Modifier.fillMaxSize(),
                        actions = listOf(
                            EmptyScreenAction(
                                stringRes = MR.strings.action_retry,
                                icon = MaterialSymbols.Rounded.Refresh,
                                onClick = viewModel::retry,
                            ),
                        ),
                    )

                    is RelatedEntriesViewModel.State.Success -> {
                        if (currentState.groups.isEmpty()) {
                            EmptyScreen(
                                message = stringResource(MR.strings.no_results_found),
                                modifier = Modifier.fillMaxSize(),
                                actions = listOf(
                                    EmptyScreenAction(
                                        stringRes = MR.strings.action_retry,
                                        icon = MaterialSymbols.Rounded.Refresh,
                                        onClick = viewModel::retry,
                                    ),
                                ),
                            )
                        } else {
                            PullRefresh(
                                refreshing = currentState.isRefreshing,
                                enabled = !currentState.isRefreshing,
                                onRefresh = viewModel::refresh,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                if (currentState.isGrouped) {
                                    RelatedEntryShelves(
                                        groups = currentState.groups,
                                        sourceItemOrientation = currentState.sourceItemOrientation,
                                        entryState = viewModel::getEntryState,
                                        onEntryClick = onEntryClick,
                                    )
                                } else {
                                    RelatedEntryGrid(
                                        entries = currentState.groups.single().entries,
                                        sourceItemOrientation = currentState.sourceItemOrientation,
                                        entryState = viewModel::getEntryState,
                                        onEntryClick = onEntryClick,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
