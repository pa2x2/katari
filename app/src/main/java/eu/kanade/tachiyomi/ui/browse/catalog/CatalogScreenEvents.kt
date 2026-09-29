package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Presents one-off catalogue events that need the user's attention. */
@Composable
internal fun CatalogScreenEventsEffect(
    screenModel: CatalogScreenModel,
    snackbarHostState: SnackbarHostState,
) {
    val keptChangesMessage = stringResource(MR.strings.browse_search_kept_filter_changes)
    val reviewLabel = stringResource(MR.strings.action_review)
    LaunchedEffect(screenModel) {
        screenModel.events.collect { event ->
            when (event) {
                CatalogScreenModel.Event.SearchKeptUnappliedFilters -> {
                    val result = snackbarHostState.showSnackbar(
                        message = keptChangesMessage,
                        actionLabel = reviewLabel,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) screenModel.openFilterSheet()
                }
            }
        }
    }
}
