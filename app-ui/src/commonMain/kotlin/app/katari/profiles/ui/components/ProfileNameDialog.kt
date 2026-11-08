package app.katari.profiles.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.field.TextField
import app.katari.profiles.core.Profile
import app.katari.profiles.core.hasNameConflict
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
            label = stringResource(if (originalProfileId == null) MR.strings.action_add else MR.strings.action_save),
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
            supportingText = if (showDuplicate) stringResource(MR.strings.profiles_name_exists) else null,
            isError = showDuplicate,
        )
    }
}
