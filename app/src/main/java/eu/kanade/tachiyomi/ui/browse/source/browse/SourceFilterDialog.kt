package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.PagingSource
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterPageLoadReason
import eu.kanade.tachiyomi.source.entry.EntryFilterPageScope
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.FilterItem
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.PagedFilterBrowseSession
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.PagedGroupFilterContent
import mihon.entry.interactions.catalogue.EntryCatalogueFilterNavigationResult
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.material.Button
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SourceFilterDialog(
    onDismissRequest: () -> Unit,
    filters: EntryFilterList,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    presets: List<SourceFeedPreset>,
    onReset: () -> Unit,
    onApplyPreset: (String) -> Unit,
    onEditPreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    canDeletePreset: (String) -> Boolean,
    onSaveAsNewPreset: (() -> Unit)? = null,
    currentPresetName: String? = null,
    onUpdateCurrentPreset: (() -> Unit)? = null,
    onFilter: () -> Unit,
    onUpdate: (EntryFilterList) -> Unit,
    onRequestSuggestions: suspend (
        EntryFilter.Autocomplete,
        EntryFilterTextInput,
    ) -> EntryCatalogueFilterSuggestionsResult,
    onRequestPagedFilterItems: (
        EntryFilter.PagedGroup<*>,
        EntryFilterPageScope,
        String?,
        EntryFilterPageLoadReason,
        String?,
    ) -> PagingSource<String, EntryFilterPageItem>,
    onRequestPagedFilterNavigation: suspend (
        EntryFilter.PagedGroup<*>,
        EntryFilterPageScope,
        String?,
    ) -> EntryCatalogueFilterNavigationResult,
    pagedFilterBrowseSession: (EntryFilter.PagedGroup<*>) -> PagedFilterBrowseSession,
    onRetry: (() -> Unit)? = null,
) {
    val updateFilters = { onUpdate(filters) }
    val rootListState = rememberLazyListState()
    var route by remember { mutableStateOf<SourceFilterRoute>(SourceFilterRoute.Root) }
    val hasPagedGroups = filters.any { it.containsPagedGroup() }
    val isError = errorMessage != null
    val slideDistance = rememberSlideDistance()
    val leavePagedGroup = { route = SourceFilterRoute.Root }
    val dismissOrLeavePagedGroup = {
        if (route is SourceFilterRoute.PagedGroup) {
            leavePagedGroup()
        } else {
            onDismissRequest()
        }
    }
    val filterAndDismiss = {
        onFilter()
        onDismissRequest()
    }

    BackHandler(enabled = route is SourceFilterRoute.PagedGroup, onBack = leavePagedGroup)

    AdaptiveSheet(
        onDismissRequest = dismissOrLeavePagedGroup,
        enableImplicitDismiss = route is SourceFilterRoute.Root,
        modifier = if (hasPagedGroups) Modifier.fillMaxHeight(0.9f) else Modifier,
    ) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                materialSharedAxisX(
                    forward = targetState is SourceFilterRoute.PagedGroup,
                    slideDistance = slideDistance,
                )
            },
            modifier = if (hasPagedGroups) Modifier.fillMaxSize() else Modifier,
            label = "sourceFilterRoute",
        ) { currentRoute ->
            when (currentRoute) {
                SourceFilterRoute.Root -> {
                    LazyColumn(state = rootListState) {
                        stickyHeader {
                            SourceFilterRootHeader(
                                presets = presets,
                                onReset = onReset,
                                onApplyPreset = onApplyPreset,
                                onEditPreset = onEditPreset,
                                onDeletePreset = onDeletePreset,
                                canDeletePreset = canDeletePreset,
                                onSaveAsNewPreset = onSaveAsNewPreset,
                                currentPresetName = currentPresetName,
                                onUpdateCurrentPreset = onUpdateCurrentPreset,
                                onFilter = filterAndDismiss,
                                resetEnabled = !isLoading,
                                filterEnabled = !isLoading && !isError,
                            )
                        }

                        if (isLoading) {
                            item {
                                HeadingItem(stringResource(MR.strings.loading))
                            }
                        } else if (isError) {
                            item {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = errorMessage)
                                    if (onRetry != null) {
                                        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                                            Text(stringResource(MR.strings.action_retry))
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filters) { filter ->
                                FilterItem(
                                    filter = filter,
                                    onUpdate = updateFilters,
                                    onOpenPagedGroup = { route = SourceFilterRoute.PagedGroup(it) },
                                    onRequestSuggestions = onRequestSuggestions,
                                )
                            }
                        }
                    }
                }
                is SourceFilterRoute.PagedGroup -> {
                    PagedGroupFilterContent(
                        filter = currentRoute.filter,
                        onBack = leavePagedGroup,
                        onFilter = filterAndDismiss,
                        onUpdate = updateFilters,
                        onRequestSuggestions = onRequestSuggestions,
                        onRequestNavigation = { scope, query ->
                            onRequestPagedFilterNavigation(currentRoute.filter, scope, query)
                        },
                        browseSession = pagedFilterBrowseSession(currentRoute.filter),
                        pagingSourceFactory = { scope, query, reason, initialAnchor ->
                            onRequestPagedFilterItems(
                                currentRoute.filter,
                                scope,
                                query,
                                reason,
                                initialAnchor,
                            )
                        },
                    )
                }
            }
        }
    }
}

private sealed interface SourceFilterRoute {
    data object Root : SourceFilterRoute

    data class PagedGroup(val filter: EntryFilter.PagedGroup<*>) : SourceFilterRoute
}

private fun EntryFilter<*>.containsPagedGroup(): Boolean {
    return when (this) {
        is EntryFilter.PagedGroup<*> -> true
        is EntryFilter.Group<*> -> state.any { (it as? EntryFilter<*>)?.containsPagedGroup() == true }
        else -> false
    }
}
