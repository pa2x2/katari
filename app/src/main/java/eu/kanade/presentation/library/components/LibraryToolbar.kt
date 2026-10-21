package eu.kanade.presentation.library.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.toPersistentList
import mihon.feature.profiles.ui.switcher.ActiveProfileButton
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Deselect
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.SelectAll
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.AppBarScrollBehavior
import mihon.presentation.kit.navigation.AppBarTitle
import mihon.presentation.kit.navigation.SearchToolbar
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.domain.library.model.LibraryGrouping
import tachiyomi.domain.library.model.LibraryGroupingDimension
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun LibraryToolbar(
    hasActiveFilters: Boolean,
    selectedCount: Int,
    title: LibraryToolbarTitle,
    currentGrouping: LibraryGrouping,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: (() -> Unit)?,
    onClickGlobalUpdate: (() -> Unit)?,
    onClickOpenRandomEntry: () -> Unit,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    scrollBehavior: AppBarScrollBehavior?,
) = when {
    selectedCount > 0 -> LibrarySelectionToolbar(
        selectedCount = selectedCount,
        onClickUnselectAll = onClickUnselectAll,
        onClickSelectAll = onClickSelectAll,
        onClickInvertSelection = onClickInvertSelection,
    )
    else -> LibraryRegularToolbar(
        title = title,
        hasFilters = hasActiveFilters,
        currentGrouping = currentGrouping,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onClickFilter = onClickFilter,
        onClickRefresh = onClickRefresh,
        onClickGlobalUpdate = onClickGlobalUpdate,
        onClickOpenRandomEntry = onClickOpenRandomEntry,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun LibraryRegularToolbar(
    title: LibraryToolbarTitle,
    hasFilters: Boolean,
    currentGrouping: LibraryGrouping,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: (() -> Unit)?,
    onClickGlobalUpdate: (() -> Unit)?,
    onClickOpenRandomEntry: () -> Unit,
    scrollBehavior: AppBarScrollBehavior?,
) {
    val updateCurrentGroupTitle = when (currentGrouping.dimensions.singleOrNull()) {
        LibraryGroupingDimension.Category -> stringResource(MR.strings.action_update_category)
        LibraryGroupingDimension.EntryType -> stringResource(MR.strings.action_update_type)
        LibraryGroupingDimension.Source -> stringResource(MR.strings.action_update_extension)
        null -> if (currentGrouping.dimensions.isEmpty()) {
            stringResource(MR.strings.action_update_library)
        } else {
            stringResource(MR.strings.action_update_group)
        }
    }
    SearchToolbar(
        titleContent = {
            AppBarTitle(
                title = title.text,
                titleSuffix = title.numberOfEntries?.let { count -> { Pill(text = "$count") } },
            )
        },
        searchQuery = searchQuery,
        onChangeSearchQuery = onSearchQueryChange,
        actions = {
            val filterTone = if (hasFilters) ContentTone.Active else ContentTone.Inherit
            AppBarActions(
                buildList<AppBar.AppBarAction> {
                    add(
                        AppBar.Action(
                            title = stringResource(MR.strings.action_filter),
                            icon = MaterialSymbols.Rounded.FilterList,
                            tone = filterTone,
                            onClick = onClickFilter,
                        ),
                    )
                    onClickGlobalUpdate?.let {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_update_library),
                                onClick = it,
                            ),
                        )
                    }
                    onClickRefresh?.let {
                        add(
                            AppBar.OverflowAction(
                                title = updateCurrentGroupTitle,
                                onClick = it,
                            ),
                        )
                    }
                    add(
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.action_open_random_manga),
                            onClick = onClickOpenRandomEntry,
                        ),
                    )
                }.toPersistentList(),
            )
            ActiveProfileButton()
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun LibrarySelectionToolbar(
    selectedCount: Int,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
) {
    AppBar(
        titleContent = { Text(text = stringResource(MR.strings.library_selected_count, selectedCount)) },
        actions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = MaterialSymbols.Rounded.SelectAll,
                        onClick = onClickSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = MaterialSymbols.Rounded.Deselect,
                        onClick = onClickInvertSelection,
                    ),
                ),
            )
        },
        isActionMode = true,
        onCancelActionMode = onClickUnselectAll,
    )
}

@Immutable
data class LibraryToolbarTitle(
    val text: String,
    val numberOfEntries: Int? = null,
)
