package eu.kanade.presentation.library.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Deselect
import app.katari.icons.materialsymbols.rounded.FilterList
import app.katari.icons.materialsymbols.rounded.SelectAll
import app.katari.icons.materialsymbols.rounded.Shuffle
import app.katari.icons.materialsymbols.rounded.Sync
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.AppBarActions
import app.katari.presentation.kit.navigation.AppBarScrollBehavior
import app.katari.presentation.kit.navigation.AppBarTitle
import app.katari.presentation.kit.navigation.SearchToolbar
import app.katari.presentation.kit.refresh.refreshAppBarAction
import app.katari.presentation.kit.surface.Pill
import app.katari.presentation.kit.theme.ContentTone
import app.katari.profiles.ui.switcher.ActiveProfileButton
import app.katari.sync.ui.status.rememberSyncToolbarAction
import kotlinx.collections.immutable.toPersistentList
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
    onClickSync: () -> Unit,
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
        onClickSync = onClickSync,
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
    onClickSync: () -> Unit,
    scrollBehavior: AppBarScrollBehavior?,
) {
    val syncAction = rememberSyncToolbarAction(onClickSync)
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
                    syncAction?.let(::add)
                    add(
                        AppBar.Action(
                            title = stringResource(MR.strings.action_filter),
                            icon = MaterialSymbols.Rounded.FilterList,
                            tone = filterTone,
                            onClick = onClickFilter,
                        ),
                    )
                    onClickRefresh?.let {
                        add(refreshAppBarAction(onClick = it, title = updateCurrentGroupTitle))
                    }
                    // An ungrouped library shows everything on one page, which the refresh icon already updates.
                    if (currentGrouping.dimensions.isNotEmpty()) {
                        onClickGlobalUpdate?.let {
                            add(
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.action_update_entire_library),
                                    onClick = it,
                                    icon = MaterialSymbols.Rounded.Sync,
                                ),
                            )
                        }
                    }
                    add(
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.action_open_random_manga),
                            onClick = onClickOpenRandomEntry,
                            icon = MaterialSymbols.Rounded.Shuffle,
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
