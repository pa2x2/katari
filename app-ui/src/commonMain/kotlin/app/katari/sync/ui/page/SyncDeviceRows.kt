package app.katari.sync.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Devices
import app.katari.icons.materialsymbols.rounded.QrCode2
import app.katari.platform.presentation.devicePlatformIcon
import app.katari.presentation.kit.menu.DropdownMenuItem
import app.katari.presentation.kit.menu.OverflowMenu
import app.katari.presentation.kit.theme.ContentTone
import app.katari.sync.client.account.AccountDevice
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.relativeTimeSpanString
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The account's devices, this one first, each with its platform, app version and last sync, and a menu that renames
 * this device or removes any other; then Link a device (D050). A device leaves by disconnecting, so its own
 * menu doesn't remove it.
 */
@Composable
internal fun devicesGroup(
    devices: SyncViewModel.DevicesState,
    // Only the server gives out link codes
    serverReachable: Boolean,
    actions: SyncPageActions,
): Preference.PreferenceGroup {
    val rows = buildList<Preference.PreferenceItem<out Any, out Any>> {
        when (devices) {
            SyncViewModel.DevicesState.Loading -> Unit
            SyncViewModel.DevicesState.Unavailable -> add(
                Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.sync_devices_unavailable)),
            )
            is SyncViewModel.DevicesState.Listed -> devices.devices.forEach { device ->
                add(
                    Preference.PreferenceItem.CustomPreference(title = "device-${device.id.value}") {
                        DeviceRow(device, actions)
                    },
                )
            }
        }
        add(
            Preference.PreferenceItem.CustomPreference(title = stringResource(MR.strings.sync_link_device)) {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.sync_link_device),
                    subtitle = stringResource(MR.strings.sync_link_device_summary),
                    icon = MaterialSymbols.Rounded.QrCode2,
                    enabled = serverReachable,
                    onPreferenceClick = actions.linkDevice,
                )
            },
        )
    }
    return Preference.PreferenceGroup(title = stringResource(MR.strings.sync_devices), preferenceItems = rows)
}

@Composable
private fun DeviceRow(device: AccountDevice, actions: SyncPageActions) {
    val description = device.device.description
    // Another device with its live channel open is reached at once, which says more than when it last synced
    val lastSynced = when {
        device.device.connected && !device.isThisDevice -> stringResource(MR.strings.sync_device_connected)
        device.device.lastSeenMillis > 0 -> relativeTimeSpanString(device.device.lastSeenMillis)
        else -> stringResource(MR.strings.sync_device_not_synced)
    }
    val summary =
        stringResource(MR.strings.sync_device_summary, description.platform, description.appVersion, lastSynced)
    TextPreferenceWidget(
        title = description.name,
        subtitle = if (device.isThisDevice) {
            stringResource(MR.strings.sync_status_with_note, stringResource(MR.strings.sync_this_device), summary)
        } else {
            summary
        },
        subtitleTone = if (device.isThisDevice) ContentTone.Primary else ContentTone.Inherit,
        icon = devicePlatformIcon(description.platform),
        widget = {
            OverflowMenu { close ->
                if (device.isThisDevice) {
                    DropdownMenuItem(
                        label = stringResource(MR.strings.action_rename),
                        onClick = {
                            close()
                            actions.renameDevice()
                        },
                    )
                } else {
                    actions.sendTestMessage?.let { send ->
                        DropdownMenuItem(
                            label = stringResource(MR.strings.sync_test_message_send),
                            onClick = {
                                close()
                                send(device)
                            },
                        )
                    }
                    DropdownMenuItem(
                        label = stringResource(MR.strings.action_remove),
                        onClick = {
                            close()
                            actions.removeDevice(device)
                        },
                    )
                }
            }
        },
    )
}
