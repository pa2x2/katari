package app.katari.sync.ui.choice

import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Devices
import app.katari.platform.presentation.devicePlatformIcon
import app.katari.presentation.kit.list.RowGroupScope
import app.katari.presentation.kit.list.SettingsRow
import app.katari.sync.client.part.OtherDeviceChoices
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The account's other devices, each saying whether it syncs what [mark] stands for, as it told the account, then how
 * many are on a version that tells nothing.
 */
internal fun RowGroupScope.otherDeviceRows(others: List<OtherDeviceChoices>, mark: SyncMark) {
    others.forEach { other ->
        val choices = other.choices ?: return@forEach
        val description = other.device.description
        row(key = other.device.id.value) {
            val says = when {
                !mark.syncedBy(choices) -> MR.strings.sync_device_keeps_own
                mark.synced -> MR.strings.sync_device_syncs_too
                else -> MR.strings.sync_device_syncs
            }
            SettingsRow(title = stringResource(says, description.name), icon = devicePlatformIcon(description.platform))
        }
    }
    val older = others.count { it.choices == null }
    if (older > 0) {
        row(key = OLDER_ROW) {
            SettingsRow(
                title = pluralStringResource(MR.plurals.sync_devices_older, older, older),
                icon = MaterialSymbols.Rounded.Devices,
            )
        }
    }
}

private const val OLDER_ROW = "older"
