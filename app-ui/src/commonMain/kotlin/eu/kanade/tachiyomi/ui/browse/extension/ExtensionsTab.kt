package eu.kanade.tachiyomi.ui.browse.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Delete
import app.katari.icons.materialsymbols.rounded.FilterList
import app.katari.icons.materialsymbols.rounded.Info
import app.katari.icons.materialsymbols.rounded.Store
import app.katari.missingextensions.ui.MissingExtensionsSheet
import app.katari.presentation.kit.dialog.ConfirmDialog
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.refresh.refreshAppBarAction
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.ExtensionFilterSheet
import eu.kanade.presentation.browse.ExtensionScreen
import eu.kanade.presentation.browse.components.ExtensionCompatibilityDialog
import eu.kanade.presentation.browse.components.ExtensionReplaceDialog
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsScreen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun extensionsTab(
    extensionsViewModel: ExtensionsViewModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val graph = LocalAppUiGraph.current
    val webViewDestination = remember { graph.webViewDestination }
    val systemPackages = remember { graph.extensionSystemPackages }
    val compatibilityFamilies = remember { graph.extensionCompatibility.families }

    val updatesCount by extensionsViewModel.updatesCount.collectAsStateWithLifecycle()
    val filter by extensionsViewModel.filter.collectAsStateWithLifecycle()
    var privateExtensionToUninstall by remember { mutableStateOf<Extension.Installed?>(null) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showCompatibility by rememberSaveable { mutableStateOf(false) }
    var showMissingExtensions by rememberSaveable { mutableStateOf(false) }
    val state by extensionsViewModel.state.collectAsStateWithLifecycle()

    return TabContent(
        titleRes = MR.strings.label_extensions,
        badgeNumber = updatesCount.takeIf { it > 0 },
        searchEnabled = true,
        actions = listOfNotNull(
            AppBar.Action(
                title = stringResource(MR.strings.action_filter),
                icon = MaterialSymbols.Rounded.FilterList,
                badgeCount = filter.activeFilterCount.takeIf { it > 0 },
                onClick = { showFilters = true },
            ),
            refreshAppBarAction(
                onClick = extensionsViewModel::findAvailableExtensions,
                enabled = !state.isLoading && !state.isRefreshing,
                title = stringResource(MR.strings.action_check_for_updates),
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
            ).takeIf { compatibilityFamilies.isNotEmpty() },
        ),
        content = { contentPadding, _ ->
            NavigationBackHandler(
                state = rememberNavigationEventState(NavigationEventInfo.None),
                isBackEnabled = state.searchQuery != null,
                onBackCompleted = { extensionsViewModel.search(null) },
            )

            ExtensionScreen(
                state = state,
                contentPadding = contentPadding,
                searchQuery = state.searchQuery,
                onLongClickItem = { extension ->
                    when (extension) {
                        is Extension.Available -> extensionsViewModel.installExtension(extension)
                        is Extension.Installed -> {
                            if (systemPackages.asksBeforeUninstall(extension)) {
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
                            webViewDestination.screen(
                                url = it.baseUrl,
                                title = it.name,
                                sourceId = it.id,
                                headers = emptyMap(),
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
                onReviewMissingExtensions = { showMissingExtensions = true },
                onOpenFilter = { showFilters = true },
                onRefresh = extensionsViewModel::findAvailableExtensions,
            )

            if (showMissingExtensions) {
                MissingExtensionsSheet(
                    onDismissRequest = { showMissingExtensions = false },
                    onOpenStores = {
                        showMissingExtensions = false
                        navigator.push(ExtensionStoresScreen())
                    },
                )
            }

            if (showCompatibility) {
                ExtensionCompatibilityDialog(
                    families = compatibilityFamilies,
                    onDismissRequest = { showCompatibility = false },
                )
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

            val replacement by extensionsViewModel.replacement.collectAsStateWithLifecycle()
            replacement?.let {
                ExtensionReplaceDialog(
                    replacement = it,
                    onConfirm = extensionsViewModel::confirmReplacement,
                    onDismissRequest = extensionsViewModel::dismissReplacement,
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
