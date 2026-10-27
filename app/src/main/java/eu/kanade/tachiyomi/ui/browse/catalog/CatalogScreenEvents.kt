package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import mihon.presentation.kit.feedback.SnackbarDuration
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.feedback.SnackbarResult
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.i18n.context.stringResource as contextStringResource

/** Presents one-off catalogue events that need the user's attention. */
@Composable
internal fun CatalogScreenEventsEffect(
    viewModel: CatalogViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val keptChangesMessage = stringResource(MR.strings.browse_search_kept_filter_changes)
    val reviewLabel = stringResource(MR.strings.action_review)
    val nothingToMergeMessage = stringResource(MR.strings.merge_target_picker_none)
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CatalogViewModel.Event.SearchKeptUnappliedFilters -> {
                    val result = snackbarHostState.showSnackbar(
                        message = keptChangesMessage,
                        actionLabel = reviewLabel,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.openFilterSheet()
                }
                CatalogViewModel.Event.NothingToMerge -> snackbarHostState.showSnackbar(nothingToMergeMessage)
                is CatalogViewModel.Event.LockedInMerge -> snackbarHostState.showSnackbar(
                    context.contextStringResource(
                        MR.strings.merge_locked_member_removal,
                        event.memberTitle,
                        event.mergedEntryTitle,
                    ),
                )
            }
        }
    }
}
