package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.components.ContentTypeFilterSummary
import eu.kanade.presentation.browse.components.ExtensionInstallErrorDialog
import eu.kanade.presentation.browse.components.ExtensionItem
import eu.kanade.presentation.browse.components.ExtensionNotLoadedDialog
import eu.kanade.presentation.browse.components.ExtensionTorrentListingDialog
import eu.kanade.presentation.browse.components.ExtensionTrustDialog
import eu.kanade.presentation.browse.components.ExtensionUnsupportedSdkListingDialog
import eu.kanade.presentation.browse.components.FilterSummaryPill
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionListState
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.platform.currentPlatformContext
import mihon.feature.missingextensions.ui.MissingExtensionsNotice
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Settings
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.EmptyScreenAction
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.refresh.PullRefresh
import mihon.presentation.kit.surface.NoticeCard
import mihon.presentation.kit.surface.NoticeTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
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
    onReinstallExtension: (Extension.Installed) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onTrustExtension: (Extension.NotLoaded) -> Unit,
    onOpenExtension: (Extension.Loaded) -> Unit,
    onClickUpdateAll: () -> Unit,
    onShowCompatibility: () -> Unit,
    onReviewMissingExtensions: () -> Unit,
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
                    onReinstallExtension = onReinstallExtension,
                    onUpdateExtension = onUpdateExtension,
                    onTrustExtension = onTrustExtension,
                    onOpenExtension = onOpenExtension,
                    onClickUpdateAll = onClickUpdateAll,
                    onShowCompatibility = onShowCompatibility,
                    onReviewMissingExtensions = onReviewMissingExtensions,
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
    onReinstallExtension: (Extension.Installed) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onTrustExtension: (Extension.NotLoaded) -> Unit,
    onOpenExtension: (Extension.Loaded) -> Unit,
    onClickUpdateAll: () -> Unit,
    onShowCompatibility: () -> Unit,
    onReviewMissingExtensions: () -> Unit,
) {
    val context = currentPlatformContext()
    val graph = LocalAppUiGraph.current
    val compatibility = remember { graph.extensionCompatibility }
    val systemPackages = remember { graph.extensionSystemPackages }
    var notLoadedState by remember { mutableStateOf<Extension.NotLoaded?>(null) }
    var torrentListingState by remember { mutableStateOf<Extension.Available?>(null) }
    var unsupportedSdkListingState by remember { mutableStateOf<Extension.Available?>(null) }
    val onInstallListing = { extension: Extension.Available ->
        when {
            extension.isTorrent -> torrentListingState = extension
            !compatibility.isInstallable(extension) -> unsupportedSdkListingState = extension
            else -> onInstallExtension(extension)
        }
    }
    var installErrorState by remember { mutableStateOf<InstallStep.Error?>(null) }
    val installPermissionMissing = systemPackages.isInstallPermissionMissing()

    RowList(
        fastScroll = true,
        contentPadding = contentPadding + PaddingValues(top = KatariTheme.spacing.s),
        wrapGroups = true,
    ) {
        if (state.filter.activeFilterCount > 0) {
            item(key = "extension-filter-summary") {
                ExtensionFilterSummary(state)
            }
        }

        if (installPermissionMissing) {
            item(key = "extension-permissions-warning") {
                NoticeCard(
                    text = stringResource(MR.strings.ext_permission_install_apps_warning),
                    tone = NoticeTone.Warning,
                    modifier = Modifier.padding(vertical = KatariTheme.spacing.s),
                    onClick = { systemPackages.requestInstallPermission(context) },
                )
            }
        }

        item(key = "missing-extensions") {
            MissingExtensionsNotice(
                onReview = onReviewMissingExtensions,
                modifier = Modifier.padding(vertical = KatariTheme.spacing.s),
            )
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
                            is Extension.Available -> onInstallListing(it)
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
                            is Extension.Available -> onInstallListing(it)
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
                sdk = remember(extension) { compatibility.unsupportedSdk(extension) },
                onClickUninstall = {
                    onUninstallExtension(extension)
                    dismiss()
                },
                onClickReinstall = {
                    onReinstallExtension(extension)
                    dismiss()
                },
                onShowCompatibility = {
                    dismiss()
                    onShowCompatibility()
                },
                onDismissRequest = dismiss,
            )
        }
    }
    torrentListingState?.let { extension ->
        ExtensionTorrentListingDialog(
            extensionName = extension.name,
            onDismissRequest = { torrentListingState = null },
        )
    }
    unsupportedSdkListingState?.let { extension ->
        val dismiss = { unsupportedSdkListingState = null }
        ExtensionUnsupportedSdkListingDialog(
            extensionName = extension.name,
            sdk = remember(extension) { compatibility.unsupportedSdk(extension) },
            onShowCompatibility = {
                dismiss()
                onShowCompatibility()
            },
            onDismissRequest = dismiss,
        )
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
