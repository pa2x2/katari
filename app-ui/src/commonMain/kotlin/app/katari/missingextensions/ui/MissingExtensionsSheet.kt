package app.katari.missingextensions.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.core.platform.currentPlatformContext
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Extension
import app.katari.missingextensions.model.CantInstall
import app.katari.missingextensions.model.MissingExtension
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonSize
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.CheckRow
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.RowGroupNote
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.list.rowGroupItems
import app.katari.presentation.kit.sheet.Sheet
import app.katari.presentation.kit.sheet.SheetActions
import app.katari.presentation.kit.sheet.SheetHeader
import app.katari.presentation.kit.theme.ContentTone
import app.katari.ui.di.LocalAppUiGraph
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.browse.components.ExtensionItemIcon
import eu.kanade.presentation.browse.components.ExtensionTrustDialog
import eu.kanade.presentation.browse.components.ExtensionWarningPill
import eu.kanade.presentation.browse.components.label
import eu.kanade.tachiyomi.extension.model.InstallStep
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Installs the extensions the library's titles need, or trusts those installed on the device but not trusted in this
 * app, from the Library card, the Sync page, Browse → Extensions or the first-run guide. Installs go on after it
 * closes. [onOpenStores] opens Extension stores, where a store that lists one is added.
 */
@Composable
fun MissingExtensionsSheet(onDismissRequest: () -> Unit, onOpenStores: () -> Unit) {
    val model = metroViewModel<MissingExtensionsSheetViewModel>()
    LaunchedEffect(Unit) { model.open() }
    val state by model.state.collectAsStateWithLifecycle()
    val graph = LocalAppUiGraph.current
    val systemPackages = remember { graph.extensionSystemPackages }
    val context = currentPlatformContext()
    val permissionMissing = systemPackages.isInstallPermissionMissing()
    var trustingKey by rememberSaveable { mutableStateOf<String?>(null) }

    Sheet(onDismissRequest = onDismissRequest, capHeight = true) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(title = stringResource(MR.strings.missing_extensions_title))
            RowGroupNote(stringResource(MR.strings.missing_extensions_note))
            val current = state ?: return@Column
            RowList(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                if (current.untrusted.isNotEmpty()) {
                    item(key = "untrusted") { RowGroupHeader(stringResource(MR.strings.missing_extensions_untrusted)) }
                    rowGroupItems(current.untrusted, key = { "untrusted-${it.key}" }) { extension ->
                        UntrustedRow(extension, current, onTrust = { trustingKey = extension.key })
                    }
                }
                if (current.installable.isNotEmpty()) {
                    item(key = "ready") { RowGroupHeader(stringResource(MR.strings.missing_extensions_ready)) }
                    rowGroupItems(current.installable, key = { it.extension.key }) { row ->
                        InstallableRow(row, current, onTickedChange = { model.setTicked(row.extension.key, it) })
                    }
                }
                if (current.unavailable.isNotEmpty()) {
                    item(key = "unavailable") {
                        RowGroupHeader(stringResource(MR.strings.missing_extensions_unavailable))
                    }
                    rowGroupItems(current.unavailable, key = { it.key }) { extension ->
                        UnavailableRow(extension, current, onOpenStores)
                    }
                }
            }
            SheetActions(
                confirm = when {
                    current.toInstall == 0 && !current.isInstalling -> null
                    permissionMissing -> DialogAction(stringResource(MR.strings.app_update_allow_installs), {
                        systemPackages.requestInstallPermission(context)
                    })
                    current.isInstalling -> DialogAction(
                        stringResource(MR.strings.ext_installing),
                        onClick = {},
                        enabled = false,
                    )
                    current.isRetry -> DialogAction(stringResource(MR.strings.action_retry), model::install)
                    else -> DialogAction(
                        pluralStringResource(
                            MR.plurals.missing_extensions_install,
                            current.toInstall,
                            current.toInstall,
                        ),
                        model::install,
                    )
                },
                dismiss = DialogAction(
                    stringResource(if (current.toInstall == 0) MR.strings.action_close else MR.strings.action_not_now),
                    onDismissRequest,
                ),
            )
        }
    }
    state?.untrusted?.find { it.key == trustingKey }?.let { extension ->
        ExtensionTrustDialog(
            onClickConfirm = {
                model.trust(extension)
                trustingKey = null
            },
            onClickDismiss = {
                model.uninstall(extension)
                trustingKey = null
            },
            onDismissRequest = { trustingKey = null },
        )
    }
}

/** An extension installed on the device but not trusted in this app, which Trust loads after the usual warning. */
@Composable
private fun UntrustedRow(
    extension: MissingExtension,
    state: MissingExtensionsSheetViewModel.State,
    onTrust: () -> Unit,
) {
    val installed = extension.untrusted ?: return
    val titles = titlesText(extension, state)
    SettingsRow(
        title = extension.name,
        subtitle = installed.store
            ?.let { stringResource(MR.strings.missing_extensions_details, titles, it.name, installed.versionName) }
            ?: titles,
        leading = { ExtensionItemIcon(installed, InstallStep.Idle) },
        trailing = {
            Button(
                label = stringResource(MR.strings.ext_trust),
                onClick = onTrust,
                style = ButtonStyle.Tonal,
                size = ButtonSize.Small,
            )
        },
    )
}

@Composable
private fun InstallableRow(
    row: MissingExtensionsSheetViewModel.InstallRow,
    state: MissingExtensionsSheetViewModel.State,
    onTickedChange: (Boolean) -> Unit,
) {
    val listing = row.extension.listing ?: return
    val step = row.step
    val subtitle = when {
        row.installed -> stringResource(MR.strings.ext_installed)
        step == InstallStep.Pending -> stringResource(MR.strings.ext_pending)
        step == InstallStep.Downloading -> stringResource(MR.strings.ext_downloading)
        step == InstallStep.Installing -> stringResource(MR.strings.ext_installing)
        step == InstallStep.Converting -> stringResource(MR.strings.ext_converting)
        step == InstallStep.RequiresUserAction -> stringResource(MR.strings.ext_requires_user_action)
        else -> stringResource(
            MR.strings.missing_extensions_details,
            titlesText(row.extension, state),
            listing.store.name,
            listing.versionName,
        )
    }
    CheckRow(
        title = row.extension.name,
        checked = row.ticked || row.installed,
        onCheckedChange = onTickedChange,
        subtitle = subtitle,
        enabled = !row.installed && step?.isCompleted() != false,
        avatar = { ExtensionItemIcon(listing, step ?: InstallStep.Idle) },
        trailing = {
            when {
                step is InstallStep.Error -> ExtensionWarningPill(
                    stringResource(MR.strings.ext_install_error),
                    ContentTone.Error,
                )
                else -> listing.contentWarning.label?.takeIf { it.onRows }?.let {
                    ExtensionWarningPill(stringResource(it.title), it.tone)
                }
            }
        },
    )
}

@Composable
private fun UnavailableRow(
    extension: MissingExtension,
    state: MissingExtensionsSheetViewModel.State,
    onOpenStores: () -> Unit,
) {
    val reason = when (extension.cantInstall) {
        CantInstall.NOT_LISTED, null -> MR.strings.missing_extensions_not_listed
        CantInstall.TORRENT -> MR.strings.missing_extensions_torrent
        CantInstall.INCOMPATIBLE -> MR.strings.missing_extensions_incompatible
    }
    val listing = extension.listing
    SettingsRow(
        title = extension.name,
        subtitle = stringResource(
            MR.strings.missing_extensions_reason,
            titlesText(extension, state),
            stringResource(reason),
        ),
        icon = MaterialSymbols.Rounded.Extension.takeIf { listing == null },
        leading = listing?.let { { ExtensionItemIcon(it, InstallStep.Idle) } },
        onClick = onOpenStores.takeIf { extension.cantInstall == CantInstall.NOT_LISTED },
    )
}

/** How many titles need [extension], naming the profiles other than the open one that hold some. */
@Composable
private fun titlesText(extension: MissingExtension, state: MissingExtensionsSheetViewModel.State): String {
    val count = extension.titleCount.toInt()
    val titles = pluralStringResource(MR.plurals.missing_extensions_titles, count, count)
    val others = extension.countsByProfile.keys
        .filter { it != state.openProfileId }
        .mapNotNull { state.profileNames[it] }
    return when {
        others.isEmpty() -> titles
        state.openProfileId in extension.countsByProfile ->
            stringResource(MR.strings.missing_extensions_also_in_profiles, titles, others.joinToString())
        else -> stringResource(MR.strings.missing_extensions_in_profiles, titles, others.joinToString())
    }
}
