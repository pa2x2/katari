package mihon.feature.profiles.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.hasNameConflict
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.TextField
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Asks for a profile's name, refusing an empty one or one another profile already has. */
@Composable
internal fun ProfileNameDialog(
    title: String,
    initialValue: String,
    existingProfiles: List<Profile>,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    originalProfileId: Long? = null,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    val trimmedValue = value.trim()
    val duplicate = remember(trimmedValue, existingProfiles, originalProfileId) {
        existingProfiles.hasNameConflict(
            name = trimmedValue,
            excludedProfileId = originalProfileId,
        )
    }
    val showDuplicate = trimmedValue.isNotEmpty() && duplicate

    Dialog(
        onDismissRequest = onDismissRequest,
        title = title,
        confirm = DialogAction(
            label = stringResource(MR.strings.action_ok),
            onClick = { onConfirm(trimmedValue) },
            enabled = trimmedValue.isNotEmpty() && !duplicate,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        TextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(MR.strings.name),
            supportingText = if (showDuplicate) {
                stringResource(MR.strings.profiles_name_exists)
            } else {
                stringResource(MR.strings.information_required_plain)
            },
            isError = showDuplicate,
        )
    }
}
