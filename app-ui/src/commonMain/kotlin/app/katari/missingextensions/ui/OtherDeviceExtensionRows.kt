package app.katari.missingextensions.ui

import androidx.compose.runtime.Composable
import app.katari.presentation.kit.list.CheckRow
import app.katari.presentation.kit.theme.ContentTone
import eu.kanade.presentation.browse.components.ExtensionItemIcon
import eu.kanade.presentation.browse.components.ExtensionWarningPill
import eu.kanade.presentation.browse.components.label
import eu.kanade.tachiyomi.extension.model.InstallStep
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** An extension another device installed, which Install installs here with the ones the library needs. */
@Composable
internal fun OtherInstallRow(
    row: MissingExtensionsSheetViewModel.OtherInstallRow,
    onTickedChange: (Boolean) -> Unit,
) {
    val listing = row.extension.listing ?: return
    val step = row.step
    CheckRow(
        title = row.extension.name,
        checked = row.ticked || row.installed,
        onCheckedChange = onTickedChange,
        subtitle = installStepText(row.installed, step) ?: stringResource(
            MR.strings.other_device_extensions_installed_on,
            row.extension.change.deviceName,
            listing.store.name,
            listing.versionName,
        ),
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

/** An extension another device uninstalled that is installed here, ticked unless titles here come from it. */
@Composable
internal fun UninstallRow(
    row: MissingExtensionsSheetViewModel.UninstallRow,
    onTickedChange: (Boolean) -> Unit,
) {
    val installed = row.extension.installed ?: return
    val device = row.extension.change.deviceName
    val titles = row.extension.titleCount.toInt()
    CheckRow(
        title = row.extension.name,
        checked = row.ticked,
        onCheckedChange = onTickedChange,
        subtitle = if (titles == 0) {
            stringResource(MR.strings.other_device_extensions_uninstalled_on, device)
        } else {
            stringResource(
                MR.strings.other_device_extensions_uninstalled_titles,
                device,
                pluralStringResource(MR.plurals.missing_extensions_titles, titles, titles),
            )
        },
        avatar = { ExtensionItemIcon(installed, InstallStep.Idle) },
    )
}
