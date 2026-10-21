package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.category.components.CategoryFloatingActionButton
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoreScreenState
import mihon.domain.extension.model.ExtensionStore
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

@Composable
fun ExtensionStoresScreen(
    state: ExtensionStoreScreenState.Success,
    onClickCreate: () -> Unit,
    onCopy: (ExtensionStore) -> Unit,
    onOpenWebsite: (ExtensionStore) -> Unit,
    onOpenDiscord: (ExtensionStore) -> Unit,
    onClickDelete: (ExtensionStore) -> Unit,
    onClickRefresh: () -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                navigateUp = navigateUp,
                title = stringResource(MR.strings.extensionStores),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        icon = MaterialSymbols.Rounded.Refresh,
                        contentDescription = stringResource(resource = MR.strings.action_webview_refresh),
                        onClick = onClickRefresh,
                    )
                },
            )
        },
        floatingActionButton = {
            CategoryFloatingActionButton(
                lazyListState = lazyListState,
                onCreate = onClickCreate,
            )
        },
    ) { paddingValues ->
        if (state.isEmpty) {
            EmptyScreen(
                MR.strings.extensionStoresScreen_emptyLabel,
                modifier = Modifier.padding(paddingValues),
            )
            return@Scaffold
        }

        ExtensionStoresContent(
            repos = state.stores,
            lazyListState = lazyListState,
            paddingValues = paddingValues + PaddingValues(top = KatariTheme.spacing.s),
            onCopy = onCopy,
            onOpenWebsite = onOpenWebsite,
            onOpenDiscord = onOpenDiscord,
            onClickDelete = onClickDelete,
        )
    }
}
