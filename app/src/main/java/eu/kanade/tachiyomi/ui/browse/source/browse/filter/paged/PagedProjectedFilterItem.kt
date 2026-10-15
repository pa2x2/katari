package eu.kanade.tachiyomi.ui.browse.source.browse.filter.paged

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import eu.kanade.tachiyomi.source.filter.withProjectedState
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.FilterItem
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import kotlinx.coroutines.launch
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.KatariTheme

/** One item of a paged list, drawn as the filter control the source projects for it. */
@Composable
internal fun ProjectedFilterItem(
    group: EntryFilter.PagedGroup<*>,
    item: EntryFilterPageItem,
    encodedState: String?,
    onEditItem: (EntryFilterPageItem, EntryFilter<*>, (Boolean) -> Unit) -> Unit,
    onSelectedItemUpdate: () -> Unit,
    selectedScope: Boolean,
    onRequestSuggestions: suspend (
        EntryFilter.Autocomplete,
        EntryFilterTextInput,
    ) -> EntryCatalogueFilterSuggestionsResult,
) {
    val scope = rememberCoroutineScope()
    var projection by remember(group, item.id) {
        mutableStateOf<ProjectedFilterItemState>(ProjectedFilterItemState.Loading)
    }
    LaunchedEffect(group, encodedState, item) {
        projection = projectFilterItemState(group, item, projection.filterOrNull())
    }

    when (val current = projection) {
        ProjectedFilterItemState.Loading -> LoadingIndicator(modifier = Modifier.padding(KatariTheme.spacing.l))
        ProjectedFilterItemState.Failed -> RetryItem {
            scope.launch { projection = projectFilterItemState(group, item, null) }
        }
        // Sources may update and return the previous leaf to preserve focus. It is the same
        // mutable instance, so FilterItem would otherwise be skipped as equal and keep showing
        // the previous visual. Keying on the durable encoded state forces a fresh FilterItem
        // composition that reads the current leaf state.
        is ProjectedFilterItemState.Ready -> key(encodedState) {
            FilterItem(
                filter = current.filter,
                changes = FilterChanges.Empty,
                onUpdate = {
                    onEditItem(item, current.filter) { success ->
                        if (!success) projection = ProjectedFilterItemState.Failed
                        if (success && selectedScope) onSelectedItemUpdate()
                    }
                },
                onOpenPagedGroup = {},
                onRequestSuggestions = onRequestSuggestions,
                changedOnly = false,
                onReset = {},
            )
        }
    }
}

private sealed interface ProjectedFilterItemState {
    data object Loading : ProjectedFilterItemState
    data class Ready(val filter: EntryFilter<*>) : ProjectedFilterItemState

    data object Failed : ProjectedFilterItemState
}

private fun ProjectedFilterItemState.filterOrNull(): EntryFilter<*>? =
    (this as? ProjectedFilterItemState.Ready)?.filter

private suspend fun projectFilterItemState(
    group: EntryFilter.PagedGroup<*>,
    item: EntryFilterPageItem,
    previous: EntryFilter<*>?,
): ProjectedFilterItemState {
    return runCatching { projectFilterItem(group, item, previous) }
        .fold(
            onSuccess = ProjectedFilterItemState::Ready,
            onFailure = { ProjectedFilterItemState.Failed },
        )
}

private suspend fun projectFilterItem(
    group: EntryFilter.PagedGroup<*>,
    item: EntryFilterPageItem,
    previous: EntryFilter<*>?,
): EntryFilter<*> {
    val projected = group.withProjectedState { it.projectItem(item, previous) }
    require(
        projected !is EntryFilter.Header &&
            projected !is EntryFilter.Separator &&
            projected !is EntryFilter.Group<*> &&
            projected !is EntryFilter.PagedGroup<*>,
    ) {
        "Paged filter items must project to interactive leaf controls"
    }
    return projected
}
