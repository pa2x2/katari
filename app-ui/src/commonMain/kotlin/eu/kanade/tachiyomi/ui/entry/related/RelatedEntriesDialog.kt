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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.core.platform.currentPlatformContext
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Refresh
import app.katari.presentation.kit.feedback.EmptyScreen
import app.katari.presentation.kit.feedback.EmptyScreenAction
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.refresh.PullRefresh
import app.katari.presentation.kit.refresh.RefreshButton
import app.katari.presentation.kit.sheet.Sheet
import app.katari.presentation.kit.sheet.SheetHeader
import app.katari.ui.di.LocalAppUiGraph
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun RelatedEntriesDialog(
    viewModel: RelatedEntriesViewModel,
    onDismissRequest: () -> Unit,
    onEntryClick: (Entry) -> Unit,
) {
    val context = currentPlatformContext()
    val errorMessages = LocalAppUiGraph.current.sourceErrorMessages
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    // A sheet capped below the window's height; the grid fills it, so loading and empty states keep its size.
    Sheet(onDismissRequest = onDismissRequest, capHeight = true) {
        Column(modifier = Modifier.fillMaxHeight()) {
            // Loading, error and empty states offer their own retry in the body.
            val results = (state as? RelatedEntriesViewModel.State.Success)?.takeIf { it.groups.isNotEmpty() }
            SheetHeader(
                title = stringResource(MR.strings.related_entries),
                actions = results?.let { success ->
                    { RefreshButton(onClick = viewModel::refresh, enabled = !success.isRefreshing) }
                },
            )
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
                        message = errorMessages.message(context, currentState.throwable),
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
