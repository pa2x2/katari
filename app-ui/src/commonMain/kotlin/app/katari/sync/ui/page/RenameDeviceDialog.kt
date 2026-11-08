package app.katari.sync.ui.page

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.field.TextField
import app.katari.sync.protocol.device.DeviceDescription
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Names this device for the account's device list; saving an empty name goes back to [platformName]. */
@Composable
internal fun RenameDeviceDialog(
    name: String,
    platformName: String,
    onRename: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(name) }
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.sync_rename_device_title),
        confirm = DialogAction(stringResource(MR.strings.action_save), onClick = { onRename(value) }),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onClick = onDismissRequest),
    ) {
        TextField(
            value = value,
            onValueChange = { value = it.take(DeviceDescription.MAX_FIELD_LENGTH) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(MR.strings.name),
            placeholder = platformName,
            supportingText = stringResource(MR.strings.sync_rename_device_hint, platformName),
        )
    }
}
