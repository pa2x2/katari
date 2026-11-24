package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Fullscreen
import app.katari.icons.materialsymbols.rounded.Public
import app.katari.icons.materialsymbols.rounded.Settings
import app.katari.platform.presentation.platformPresentation
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.AppBarActions
import app.katari.presentation.kit.navigation.AppBarScrollBehavior
import app.katari.presentation.kit.navigation.AppBarTitle
import app.katari.presentation.kit.navigation.SearchToolbar
import app.katari.presentation.kit.refresh.refreshAppBarAction
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun CatalogToolbar(
    title: String,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    displayAction: AppBar.Action,
    navigateUp: () -> Unit,
    onWebViewClick: (() -> Unit)?,
    onSettingsClick: (() -> Unit)?,
    onLongPressActionsClick: () -> Unit,
    onSearch: (String) -> Unit,
    onEnterImmersive: (() -> Unit)?,
    onRefresh: () -> Unit,
    refreshEnabled: Boolean,
    scrollBehavior: AppBarScrollBehavior,
) {
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
                    add(displayAction)
                    add(refreshAppBarAction(onClick = onRefresh, enabled = refreshEnabled))
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
                            title = stringResource(platformPresentation.longPressActionsLabel),
                            onClick = onLongPressActionsClick,
                            icon = platformPresentation.pointerIcon,
                        ),
                    )
                },
            )
        },
    )
}
