package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.components.ContentTypeFilterSummary
import eu.kanade.presentation.browse.components.ExtensionInstallErrorDialog
import eu.kanade.presentation.browse.components.ExtensionItem
import eu.kanade.presentation.browse.components.ExtensionNotLoadedDialog
import eu.kanade.presentation.browse.components.ExtensionTrustDialog
import eu.kanade.presentation.browse.components.FilterSummaryPill
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.presentation.util.rememberRequestPackageInstallsPermissionState
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionListState
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel
import eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Settings
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.EmptyScreenAction
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.progress.PullRefresh
import mihon.presentation.kit.surface.WarningCard
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

@Composable
fun ExtensionScreen(
    state: ExtensionListState,
    contentPadding: PaddingValues,
    searchQuery: String?,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onInstallExtension: (Extension.Available) -> Unit,
    onUninstallExtension: (Extension.Installed) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onTrustExtension: (Extension.NotLoaded) -> Unit,
    onOpenExtension: (Extension.Loaded) -> Unit,
    onClickUpdateAll: () -> Unit,
    onRefresh: () -> Unit,
) {
    val navigator = LocalNavigator.currentOrThrow

    PullRefresh(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh,
        enabled = !state.isLoading,
    ) {
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
            state.isEmpty -> {
                val msg = if (!searchQuery.isNullOrEmpty() || state.filter.activeFilterCount > 0) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.empty_screen
                }
                EmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                    actions = listOf(
                        EmptyScreenAction(
                            stringRes = MR.strings.extensionStores,
                            icon = MaterialSymbols.Rounded.Settings,
                            onClick = { navigator.push(ExtensionStoresScreen()) },
                        ),
                    ),
                )
            }
            else -> {
                ExtensionContent(
                    state = state,
                    contentPadding = contentPadding,
                    onLongClickItem = onLongClickItem,
                    onClickItemCancel = onClickItemCancel,
                    onOpenWebView = onOpenWebView,
                    onInstallExtension = onInstallExtension,
                    onUninstallExtension = onUninstallExtension,
                    onUpdateExtension = onUpdateExtension,
                    onTrustExtension = onTrustExtension,
                    onOpenExtension = onOpenExtension,
                    onClickUpdateAll = onClickUpdateAll,
                )
            }
        }
    }
}

@Composable
private fun ExtensionContent(
    state: ExtensionListState,
    contentPadding: PaddingValues,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onInstallExtension: (Extension.Available) -> Unit,
    onUninstallExtension: (Extension.Installed) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onTrustExtension: (Extension.NotLoaded) -> Unit,
    onOpenExtension: (Extension.Loaded) -> Unit,
    onClickUpdateAll: () -> Unit,
) {
    val context = LocalContext.current
    var notLoadedState by remember { mutableStateOf<Extension.NotLoaded?>(null) }
    var installErrorState by remember { mutableStateOf<InstallStep.Error?>(null) }
    val installGranted = rememberRequestPackageInstallsPermissionState(initialValue = true)

    FastScrollLazyColumn(
        contentPadding = contentPadding + PaddingValues(top = KatariTheme.spacing.s),
    ) {
        if (state.filter.activeFilterCount > 0) {
            item(key = "extension-filter-summary") {
                ExtensionFilterSummary(state)
            }
        }

        if (!installGranted && state.installer?.requiresSystemPermission == true) {
            item(key = "extension-permissions-warning") {
                WarningCard(
                    text = stringResource(MR.strings.ext_permission_install_apps_warning),
                    modifier = Modifier.padding(vertical = KatariTheme.spacing.s),
                    onClick = { context.launchRequestPackageInstallsPermission() },
                )
            }
        }

        state.items.forEach { (header, items) ->
            item(
                contentType = "header",
                key = "extensionHeader-${header.hashCode()}",
            ) {
                when (header) {
                    is ExtensionUiModel.Header.Resource -> RowGroupHeader(
                        text = stringResource(header.textRes),
                        modifier = Modifier.animateItem(),
                        actions = if (header.textRes == MR.strings.ext_updates_pending) {
                            {
                                Button(
                                    label = stringResource(MR.strings.ext_update_all),
                                    onClick = onClickUpdateAll,
                                )
                            }
                        } else {
                            null
                        },
                    )
                    is ExtensionUiModel.Header.Text -> RowGroupHeader(
                        text = header.text,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            rowGroupItems(
                items = items,
                contentType = { "item" },
                key = { item ->
                    when (item.extension) {
                        is Extension.NotLoaded -> "extension-not-loaded-${item.hashCode()}"
                        is Extension.Loaded -> "extension-loaded-${item.hashCode()}"
                        is Extension.Available -> "extension-available-${item.hashCode()}"
                    }
                },
            ) { item ->
                ExtensionItem(
                    item = item,
                    onClickItem = {
                        when (it) {
                            is Extension.Available -> onInstallExtension(it)
                            is Extension.Loaded -> onOpenExtension(it)
                            is Extension.NotLoaded -> {
                                notLoadedState = it
                            }
                        }
                    },
                    onLongClickItem = onLongClickItem,
                    onClickItemSecondaryAction = {
                        when (it) {
                            is Extension.Available -> onOpenWebView(it)
                            is Extension.Loaded -> onOpenExtension(it)
                            is Extension.NotLoaded -> {
                                notLoadedState = it
                            }
                        }
                    },
                    onClickItemCancel = onClickItemCancel,
                    onClickItemError = { installErrorState = it },
                    onClickItemAction = {
                        when (it) {
                            is Extension.Available -> onInstallExtension(it)
                            is Extension.Loaded -> {
                                if (it.hasUpdate) {
                                    onUpdateExtension(it)
                                } else {
                                    onOpenExtension(it)
                                }
                            }
                            is Extension.NotLoaded -> {
                                if (it.hasUpdate) {
                                    onUpdateExtension(it)
                                } else {
                                    notLoadedState = it
                                }
                            }
                        }
                    },
                )
            }
        }
    }
    notLoadedState?.let { extension ->
        val dismiss = { notLoadedState = null }
        if (extension.reason is Extension.NotLoaded.Reason.Untrusted) {
            ExtensionTrustDialog(
                onClickConfirm = {
                    onTrustExtension(extension)
                    dismiss()
                },
                onClickDismiss = {
                    onUninstallExtension(extension)
                    dismiss()
                },
                onDismissRequest = dismiss,
            )
        } else {
            ExtensionNotLoadedDialog(
                extensionName = extension.name,
                reason = extension.reason,
                onClickUninstall = {
                    onUninstallExtension(extension)
                    dismiss()
                },
                onDismissRequest = dismiss,
            )
        }
    }
    installErrorState?.let { error ->
        ExtensionInstallErrorDialog(
            error = error,
            onDismissRequest = { installErrorState = null },
        )
    }
}

@Composable
private fun ExtensionFilterSummary(state: ExtensionListState) {
    ContentTypeFilterSummary(filter = state.filter.contentTypes) {
        if (state.filter.stores.isActive) {
            FilterSummaryPill(
                pluralStringResource(
                    MR.plurals.extension_filter_stores_count,
                    count = state.filter.stores.enabledStoreCount,
                    state.filter.stores.enabledStoreCount,
                ),
            )
        }
        FilterSummaryPill(
            pluralStringResource(
                MR.plurals.extension_filter_languages_count,
                count = state.visibleLanguageCount,
                state.visibleLanguageCount,
            ),
        )
    }
}
