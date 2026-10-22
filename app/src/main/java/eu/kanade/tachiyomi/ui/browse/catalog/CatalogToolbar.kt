package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredroundedfilled.ViewList
import mihon.icons.materialsymbols.rounded.Fullscreen
import mihon.icons.materialsymbols.rounded.Public
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.TouchApp
import mihon.icons.materialsymbols.roundedfilled.ViewCompact
import mihon.icons.materialsymbols.roundedfilled.ViewModule
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.RadioMenuItem
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.AppBarScrollBehavior
import mihon.presentation.kit.navigation.AppBarTitle
import mihon.presentation.kit.navigation.SearchToolbar
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun CatalogToolbar(
    title: String,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    displayMode: LibraryDisplayMode,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
    navigateUp: () -> Unit,
    onWebViewClick: (() -> Unit)?,
    onSettingsClick: (() -> Unit)?,
    onLongPressActionsClick: () -> Unit,
    onSearch: (String) -> Unit,
    onEnterImmersive: (() -> Unit)?,
    scrollBehavior: AppBarScrollBehavior,
) {
    var selectingDisplayMode by remember { mutableStateOf(false) }

    SearchToolbar(
        navigateUp = navigateUp,
        titleContent = { AppBarTitle(title) },
        searchQuery = searchQuery,
        onChangeSearchQuery = onSearchQueryChange,
        onSearch = onSearch,
        onClickCloseSearch = navigateUp,
        scrollBehavior = scrollBehavior,
        actions = {
            AppBarActions(
                actions = buildList {
                    onEnterImmersive?.let {
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.browse_enter_immersive),
                                icon = MaterialSymbols.Rounded.Fullscreen,
                                onClick = it,
                            ),
                        )
                    }
                    add(
                        AppBar.Action(
                            title = stringResource(MR.strings.action_display_mode),
                            icon = when (displayMode) {
                                LibraryDisplayMode.List,
                                LibraryDisplayMode.ComfortableList,
                                -> MaterialSymbols.AutoMirroredRoundedFilled.ViewList
                                LibraryDisplayMode.ComfortableGrid -> MaterialSymbols.RoundedFilled.ViewModule
                                LibraryDisplayMode.CompactGrid,
                                LibraryDisplayMode.CoverOnlyGrid,
                                -> MaterialSymbols.RoundedFilled.ViewCompact
                            },
                            onClick = { selectingDisplayMode = true },
                            menu = {
                                DropdownMenu(
                                    expanded = selectingDisplayMode,
                                    onDismissRequest = { selectingDisplayMode = false },
                                ) {
                                    RadioMenuItem(
                                        label = stringResource(MR.strings.action_display_comfortable_grid),
                                        selected = displayMode == LibraryDisplayMode.ComfortableGrid,
                                        onClick = {
                                            selectingDisplayMode = false
                                            onDisplayModeChange(LibraryDisplayMode.ComfortableGrid)
                                        },
                                    )
                                    RadioMenuItem(
                                        label = stringResource(MR.strings.action_display_comfortable_list),
                                        selected = displayMode == LibraryDisplayMode.ComfortableList,
                                        onClick = {
                                            selectingDisplayMode = false
                                            onDisplayModeChange(LibraryDisplayMode.ComfortableList)
                                        },
                                    )
                                    RadioMenuItem(
                                        label = stringResource(MR.strings.action_display_grid),
                                        selected = displayMode == LibraryDisplayMode.CompactGrid,
                                        onClick = {
                                            selectingDisplayMode = false
                                            onDisplayModeChange(LibraryDisplayMode.CompactGrid)
                                        },
                                    )
                                    RadioMenuItem(
                                        label = stringResource(MR.strings.action_display_list),
                                        selected = displayMode == LibraryDisplayMode.List,
                                        onClick = {
                                            selectingDisplayMode = false
                                            onDisplayModeChange(LibraryDisplayMode.List)
                                        },
                                    )
                                }
                            },
                        ),
                    )
                    onWebViewClick?.let {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_open_in_web_view),
                                onClick = it,
                                icon = MaterialSymbols.Rounded.Public,
                            ),
                        )
                    }
                    onSettingsClick?.let {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_settings),
                                onClick = it,
                                icon = MaterialSymbols.Rounded.Settings,
                            ),
                        )
                    }
                    add(
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.pref_browse_long_press_action_open_settings),
                            onClick = onLongPressActionsClick,
                            icon = MaterialSymbols.Rounded.TouchApp,
                        ),
                    )
                },
            )
        },
    )
}
