package app.katari.sync.ui.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Download
import app.katari.platform.presentation.devicePlatformIcon
import app.katari.presentation.kit.sheet.PickerChoice
import app.katari.presentation.kit.sheet.PickerPopover
import app.katari.presentation.kit.theme.ContentTone
import app.katari.sync.client.downloadrequest.DownloadTarget
import app.katari.sync.client.downloadrequest.DownloadTargetReadiness
import eu.kanade.presentation.util.relativeTimeSpanString
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The picker at [pick]'s step: the account's other devices, then, for an entry's Download menu, how much. */
@Composable
internal fun DownloadElsewherePicker(pick: DownloadElsewherePick, elsewhere: DownloadElsewhere) {
    val subject = pick.request.subject
    when (pick) {
        is DownloadElsewherePick.Devices -> {
            val devicesFlow = remember(pick.request.sourceId) { elsewhere.viewModel.devicesFor(pick.request.sourceId) }
            val devices by devicesFlow.collectAsState(
                initial = DownloadElsewhereDevices(elsewhere.listedDevices, extensionName = null),
            )
            PickerPopover(
                anchor = pick.anchor,
                title = stringResource(MR.strings.sync_download_elsewhere_title),
                subtitle = subject,
                choices = devices.targets.map { deviceChoice(it, devices.extensionName, elsewhere) },
                onDismissRequest = { elsewhere.pick = null },
            )
        }
        is DownloadElsewherePick.Amounts -> PickerPopover(
            anchor = pick.anchor,
            title = stringResource(MR.strings.sync_download_on, pick.device.description.name),
            subtitle = subject,
            choices = pick.request.amounts.map { amount ->
                PickerChoice(
                    key = amount.label,
                    title = amount.label,
                    icon = MaterialSymbols.Rounded.Download,
                    onClick = { elsewhere.pickedAmount(pick.device, amount) },
                )
            },
            onDismissRequest = { elsewhere.pick = null },
            onBack = if (pick.canGoBack) elsewhere::back else null,
        )
    }
}

@Composable
private fun deviceChoice(target: DownloadTarget, extensionName: String?, elsewhere: DownloadElsewhere): PickerChoice {
    val device = target.device
    // A device that can take the chapters says when it would get them, and one that can't says why
    val subtitle = when (target.readiness) {
        DownloadTargetReadiness.NEEDS_NEWER_APP -> stringResource(MR.strings.sync_download_needs_newer_app)
        DownloadTargetReadiness.MISSING_EXTENSION ->
            stringResource(MR.strings.sync_download_missing_extension, extensionName.orEmpty())
        DownloadTargetReadiness.READY -> when {
            device.connected -> stringResource(MR.strings.sync_device_connected)
            device.lastSeenMillis > 0 -> relativeTimeSpanString(device.lastSeenMillis)
            else -> stringResource(MR.strings.sync_device_not_synced)
        }
    }
    return PickerChoice(
        key = device.id.value,
        title = device.description.name,
        icon = devicePlatformIcon(device.description.platform),
        onClick = { elsewhere.picked(device) },
        subtitle = subtitle,
        subtitleTone = if (target.readiness == DownloadTargetReadiness.READY && device.connected) {
            ContentTone.Primary
        } else {
            ContentTone.Inherit
        },
        enabled = target.readiness == DownloadTargetReadiness.READY,
    )
}
