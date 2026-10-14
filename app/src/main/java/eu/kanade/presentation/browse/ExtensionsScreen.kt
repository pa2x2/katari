package eu.kanade.presentation.browse

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.browse.components.BaseBrowseItem
import eu.kanade.presentation.browse.components.ContentTypeFilterSummary
import eu.kanade.presentation.browse.components.ExtensionIcon
import eu.kanade.presentation.browse.components.ExtensionInstallErrorDialog
import eu.kanade.presentation.browse.components.ExtensionNotLoadedDialog
import eu.kanade.presentation.browse.components.SourceEntryTypeIndicators
import eu.kanade.presentation.browse.components.label
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.presentation.util.rememberRequestPackageInstallsPermissionState
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionListState
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel
import eu.kanade.tachiyomi.ui.browse.extension.supportedEntryTypesForDisplay
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.OpenInBrowser
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.VerifiedUser
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.DotSeparatorNoSpaceText
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.LoadingIndicator
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.secondaryItemAlpha

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
        contentPadding = contentPadding + topSmallPaddingValues,
    ) {
        if (state.filter.activeFilterCount > 0) {
            item(key = "extension-filter-summary") {
                ExtensionFilterSummary(state)
            }
        }

        if (!installGranted && state.installer?.requiresSystemPermission == true) {
            item(key = "extension-permissions-warning") {
                WarningBanner(
                    textRes = MR.strings.ext_permission_install_apps_warning,
                    modifier = Modifier.clickable {
                        context.launchRequestPackageInstallsPermission()
                    },
                )
            }
        }

        state.items.forEach { (header, items) ->
            item(
                contentType = "header",
                key = "extensionHeader-${header.hashCode()}",
            ) {
                when (header) {
                    is ExtensionUiModel.Header.Resource -> {
                        val action: @Composable RowScope.() -> Unit =
                            if (header.textRes == MR.strings.ext_updates_pending) {
                                {
                                    Button(onClick = { onClickUpdateAll() }) {
                                        Text(
                                            text = stringResource(MR.strings.ext_update_all),
                                            style = LocalTextStyle.current.copy(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                        )
                                    }
                                }
                            } else {
                                {}
                            }
                        ExtensionHeader(
                            textRes = header.textRes,
                            modifier = Modifier.animateItem(),
                            action = action,
                        )
                    }
                    is ExtensionUiModel.Header.Text -> {
                        ExtensionHeader(
                            text = header.text,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            items(
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
                    modifier = Modifier.animateItem(),
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
            Text(
                text = pluralStringResource(
                    MR.plurals.extension_filter_stores_count,
                    count = state.filter.stores.enabledStoreCount,
                    state.filter.stores.enabledStoreCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = MaterialTheme.padding.extraSmall),
            )
        }
        Text(
            text = pluralStringResource(
                MR.plurals.extension_filter_languages_count,
                count = state.visibleLanguageCount,
                state.visibleLanguageCount,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = MaterialTheme.padding.extraSmall),
        )
    }
}

@Composable
private fun ExtensionItem(
    item: ExtensionUiModel.Item,
    onClickItem: (Extension) -> Unit,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onClickItemError: (InstallStep.Error) -> Unit,
    onClickItemAction: (Extension) -> Unit,
    onClickItemSecondaryAction: (Extension) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (extension, installStep) = item
    BaseBrowseItem(
        modifier = modifier
            .combinedClickable(
                onClick = { onClickItem(extension) },
                onLongClick = { onLongClickItem(extension) },
            ),
        onClickItem = { onClickItem(extension) },
        onLongClickItem = { onLongClickItem(extension) },
        icon = {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                val idle = installStep.isCompleted()
                if (!idle) {
                    LoadingIndicator(
                        modifier = Modifier.size(40.dp),
                    )
                }

                val padding by animateDpAsState(
                    targetValue = if (idle) 0.dp else 8.dp,
                    label = "iconPadding",
                )
                ExtensionIcon(
                    extension = extension,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(padding),
                )
            }
        },
        action = {
            ExtensionItemActions(
                extension = extension,
                installStep = installStep,
                onClickItemCancel = onClickItemCancel,
                onClickItemError = onClickItemError,
                onClickItemAction = onClickItemAction,
                onClickItemSecondaryAction = onClickItemSecondaryAction,
            )
        },
    ) {
        ExtensionItemContent(
            extension = extension,
            installStep = installStep,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExtensionItemContent(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = MaterialTheme.padding.medium),
    ) {
        Text(
            text = extension.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        val store = when (extension) {
            is Extension.Available -> extension.store
            is Extension.Installed -> extension.store
        }
        if (store != null) {
            Text(
                text = store.name,
                modifier = Modifier.secondaryItemAlpha(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        // Won't look good but it's not like we can ellipsize overflowing content
        FlowRow(
            modifier = Modifier.secondaryItemAlpha(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                var hasAlreadyShownAnElement by remember { mutableStateOf(false) }
                if (extension is Extension.Loaded && extension.lang.isNotEmpty()) {
                    hasAlreadyShownAnElement = true
                    Text(
                        text = LocaleHelper.getSourceDisplayName(extension.lang, LocalContext.current),
                    )
                }

                if (extension.versionName.isNotEmpty()) {
                    if (hasAlreadyShownAnElement) DotSeparatorNoSpaceText()
                    hasAlreadyShownAnElement = true
                    Text(
                        text = extension.versionName,
                    )
                }

                val warnings = listOfNotNull(
                    (installStep as? InstallStep.Error)?.let {
                        MR.strings.ext_install_error to
                            MaterialTheme.colorScheme.error
                    },
                    when {
                        extension is Extension.NotLoaded ->
                            extension.reason.labelRes?.let { it to MaterialTheme.colorScheme.error }
                        extension is Extension.Loaded && extension.isObsolete ->
                            MR.strings.ext_obsolete to MaterialTheme.colorScheme.error
                        else -> null
                    },
                    extension.contentWarning.label?.let { it.title to it.color },
                )
                warnings.forEach { (label, color) ->
                    if (hasAlreadyShownAnElement) DotSeparatorNoSpaceText()
                    hasAlreadyShownAnElement = true
                    Text(
                        text = stringResource(label).uppercase(),
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (extension is Extension.Loaded && !extension.isShared) {
                    if (hasAlreadyShownAnElement) DotSeparatorNoSpaceText()
                    Text(
                        text = stringResource(MR.strings.ext_installer_private),
                    )
                }

                if (!installStep.isCompleted()) {
                    DotSeparatorNoSpaceText()
                    Text(
                        text = when (installStep) {
                            InstallStep.Pending -> stringResource(MR.strings.ext_pending)
                            InstallStep.Downloading -> stringResource(MR.strings.ext_downloading)
                            InstallStep.Installing -> stringResource(MR.strings.ext_installing)
                            else -> error("Must not show non-install process text")
                        },
                    )
                } else if (installStep == InstallStep.RequiresUserAction) {
                    DotSeparatorNoSpaceText()
                    Text(
                        text = stringResource(MR.strings.ext_requires_user_action),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtensionItemActions(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
    onClickItemCancel: (Extension) -> Unit = {},
    onClickItemError: (InstallStep.Error) -> Unit = {},
    onClickItemAction: (Extension) -> Unit = {},
    onClickItemSecondaryAction: (Extension) -> Unit = {},
) {
    val isIdle = installStep.isCompleted()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceEntryTypeIndicators(extension.supportedEntryTypesForDisplay())
        when {
            !isIdle -> {
                IconButton(onClick = { onClickItemCancel(extension) }) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Close,
                        contentDescription = stringResource(MR.strings.action_cancel),
                    )
                }
            }
            installStep is InstallStep.Error -> {
                IconButton(onClick = { onClickItemError(installStep) }) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Info,
                        contentDescription = stringResource(MR.strings.ext_install_error_details),
                    )
                }
                IconButton(onClick = { onClickItemAction(extension) }) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Refresh,
                        contentDescription = stringResource(MR.strings.action_retry),
                    )
                }
            }
            installStep == InstallStep.RequiresUserAction -> {
                IconButton(onClick = { onClickItemAction(extension) }) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Download,
                        contentDescription = stringResource(MR.strings.ext_update),
                    )
                }
            }
            installStep == InstallStep.Idle -> {
                when (extension) {
                    is Extension.Loaded -> {
                        IconButton(onClick = { onClickItemSecondaryAction(extension) }) {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Settings,
                                contentDescription = stringResource(MR.strings.action_settings),
                            )
                        }

                        if (extension.hasUpdate) {
                            IconButton(onClick = { onClickItemAction(extension) }) {
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.Download,
                                    contentDescription = stringResource(MR.strings.ext_update),
                                )
                            }
                        }
                    }
                    is Extension.NotLoaded -> {
                        val isUntrusted = extension.reason is Extension.NotLoaded.Reason.Untrusted
                        IconButton(onClick = { onClickItemSecondaryAction(extension) }) {
                            Icon(
                                imageVector = if (isUntrusted) {
                                    MaterialSymbols.Rounded.VerifiedUser
                                } else {
                                    MaterialSymbols.Rounded.Info
                                },
                                contentDescription = if (isUntrusted) {
                                    stringResource(MR.strings.ext_trust)
                                } else {
                                    stringResource(MR.strings.ext_not_loaded_details)
                                },
                            )
                        }

                        if (extension.hasUpdate) {
                            IconButton(onClick = { onClickItemAction(extension) }) {
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.Download,
                                    contentDescription = stringResource(MR.strings.ext_update),
                                )
                            }
                        }
                    }
                    is Extension.Available -> {
                        if (extension.sources.isNotEmpty()) {
                            IconButton(
                                onClick = { onClickItemSecondaryAction(extension) },
                            ) {
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.OpenInBrowser,
                                    contentDescription = stringResource(MR.strings.action_open_in_web_view),
                                )
                            }
                        }

                        IconButton(onClick = { onClickItemAction(extension) }) {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Download,
                                contentDescription = stringResource(MR.strings.ext_install),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtensionHeader(
    textRes: StringResource,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {},
) {
    ExtensionHeader(
        text = stringResource(textRes),
        modifier = modifier,
        action = action,
    )
}

@Composable
private fun ExtensionHeader(
    text: String,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .weight(1f),
            style = MaterialTheme.typography.header,
        )
        action()
    }
}

/**
 * Only the reasons the user can act on are worth naming in the row; the rest all mean "broken" to
 * them and are spelled out in [ExtensionNotLoadedDialog] instead.
 */
private val Extension.NotLoaded.Reason.labelRes: StringResource?
    get() = when (this) {
        is Extension.NotLoaded.Reason.Untrusted -> MR.strings.ext_untrusted
        Extension.NotLoaded.Reason.Filtered -> MR.strings.ext_filtered
        // The section header already says these aren't loaded; the dialog says why
        Extension.NotLoaded.Reason.Unsigned,
        Extension.NotLoaded.Reason.UnsupportedLibVersion,
        Extension.NotLoaded.Reason.Malformed,
        is Extension.NotLoaded.Reason.Failed,
        -> null
    }

@Composable
private fun ExtensionTrustDialog(
    onClickConfirm: () -> Unit,
    onClickDismiss: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = stringResource(MR.strings.untrusted_extension))
        },
        text = {
            Text(text = stringResource(MR.strings.untrusted_extension_message))
        },
        confirmButton = {
            TextButton(onClick = onClickConfirm) {
                Text(text = stringResource(MR.strings.ext_trust))
            }
        },
        dismissButton = {
            TextButton(onClick = onClickDismiss) {
                Text(text = stringResource(MR.strings.ext_uninstall))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}
