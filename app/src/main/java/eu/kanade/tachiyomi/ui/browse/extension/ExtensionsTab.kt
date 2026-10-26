package eu.kanade.tachiyomi.ui.browse.extension

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.ExtensionFilterSheet
import eu.kanade.presentation.browse.ExtensionScreen
import eu.kanade.presentation.browse.components.ExtensionCompatibilityDialog
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import eu.kanade.tachiyomi.util.system.isPackageInstalled
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.Store
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.navigation.AppBar
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun extensionsTab(
    extensionsViewModel: ExtensionsViewModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current

    val updatesCount by extensionsViewModel.updatesCount.collectAsStateWithLifecycle()
    val filter by extensionsViewModel.filter.collectAsStateWithLifecycle()
    var privateExtensionToUninstall by remember { mutableStateOf<Extension.Installed?>(null) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showCompatibility by rememberSaveable { mutableStateOf(false) }

    return TabContent(
        titleRes = MR.strings.label_extensions,
        badgeNumber = updatesCount.takeIf { it > 0 },
        searchEnabled = true,
        actions = listOf(
            AppBar.Action(
                title = stringResource(MR.strings.action_filter),
                icon = MaterialSymbols.Rounded.FilterList,
                badgeCount = filter.activeFilterCount.takeIf { it > 0 },
                onClick = { showFilters = true },
            ),
            AppBar.OverflowAction(
                title = stringResource(MR.strings.extensionStores),
                onClick = { navigator.push(ExtensionStoresScreen()) },
                icon = MaterialSymbols.Rounded.Store,
            ),
            AppBar.OverflowAction(
                title = stringResource(MR.strings.ext_compatibility),
                onClick = { showCompatibility = true },
                icon = MaterialSymbols.Rounded.Info,
            ),
        ),
        content = { contentPadding, _ ->
            val state by extensionsViewModel.state.collectAsStateWithLifecycle()

            BackHandler(enabled = state.searchQuery != null) {
                extensionsViewModel.search(null)
            }

            ExtensionScreen(
                state = state,
                contentPadding = contentPadding,
                searchQuery = state.searchQuery,
                onLongClickItem = { extension ->
                    when (extension) {
                        is Extension.Available -> extensionsViewModel.installExtension(extension)
                        is Extension.Installed -> {
                            if (context.isPackageInstalled(extension.pkgName)) {
                                extensionsViewModel.uninstallExtension(extension)
                            } else {
                                privateExtensionToUninstall = extension
                            }
                        }
                    }
                },
                onClickItemCancel = extensionsViewModel::cancelInstallUpdateExtension,
                onClickUpdateAll = extensionsViewModel::updateAllExtensions,
                onOpenWebView = { extension ->
                    extension.sources.getOrNull(0)?.let {
                        navigator.push(
                            WebViewScreen(
                                url = it.baseUrl,
                                initialTitle = it.name,
                                sourceId = it.id,
                            ),
                        )
                    }
                },
                onInstallExtension = { extension ->
                    extensionsViewModel.installExtension(extension)
                },
                onOpenExtension = { extension ->
                    navigator.push(ExtensionDetailsScreen(extension.pkgName))
                },
                onTrustExtension = { extensionsViewModel.trustExtension(it) },
                onUninstallExtension = { extensionsViewModel.uninstallExtension(it) },
                onReinstallExtension = { extensionsViewModel.reinstallExtension(it) },
                onUpdateExtension = { extension ->
                    extensionsViewModel.updateExtension(extension)
                },
                onShowCompatibility = { showCompatibility = true },
                onRefresh = extensionsViewModel::findAvailableExtensions,
            )

            if (showCompatibility) {
                ExtensionCompatibilityDialog(onDismissRequest = { showCompatibility = false })
            }

            if (showFilters) {
                ExtensionFilterSheet(
                    state = filter,
                    onDismissRequest = { showFilters = false },
                    onReset = extensionsViewModel::resetFilters,
                    onShowAllContentTypes = extensionsViewModel::showAllContentTypes,
                    onToggleContentType = extensionsViewModel::toggleContentType,
                    onToggleUnspecifiedContentType = extensionsViewModel::toggleUnspecifiedContentType,
                    onToggleStore = extensionsViewModel::toggleStore,
                    onToggleLanguage = extensionsViewModel::toggleLanguage,
                )
            }

            privateExtensionToUninstall?.let { extension ->
                ExtensionUninstallConfirmation(
                    extensionName = extension.name,
                    onClickConfirm = {
                        extensionsViewModel.uninstallExtension(extension)
                    },
                    onDismissRequest = {
                        privateExtensionToUninstall = null
                    },
                )
            }
        },
    )
}

@Composable
private fun ExtensionUninstallConfirmation(
    extensionName: String,
    onClickConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(MR.strings.ext_confirm_remove),
        text = stringResource(MR.strings.remove_private_extension_message, extensionName),
        confirmLabel = stringResource(MR.strings.ext_remove),
        onConfirm = {
            onClickConfirm()
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
        destructive = true,
        icon = MaterialSymbols.Rounded.Delete,
    )
}
