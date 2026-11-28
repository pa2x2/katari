package app.katari.profiles.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.field.TextField
import app.katari.profiles.core.Profile
import app.katari.profiles.core.hasNameConflict
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Asks for a profile's name, refusing an empty one or one another profile already has. The field opens empty with
 * [suggestion] greyed in it, the name kept when nothing is typed, such as a new profile's suggested name or the
 * current one when renaming (D121). [confirmLabel] says what confirming does: Add makes a new profile, Save keeps a
 * name.
 */
@Composable
internal fun ProfileNameDialog(
    title: String,
    suggestion: String,
    existingProfiles: List<Profile>,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    originalProfileId: Long? = null,
    confirmLabel: String = stringResource(
        if (originalProfileId ==
            null
        ) {
            MR.strings.action_add
        } else {
            MR.strings.action_save
        },
    ),
) {
    val focusRequester = remember { FocusRequester() }
    var value by rememberSaveable { mutableStateOf("") }
    val trimmedValue = value.trim().ifEmpty { suggestion.trim() }
    val duplicate = remember(trimmedValue, existingProfiles, originalProfileId) {
        existingProfiles.hasNameConflict(
            name = trimmedValue,
            excludedProfileId = originalProfileId,
        )
    }
    val showDuplicate = trimmedValue.isNotEmpty() && duplicate

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = title,
        confirm = DialogAction(
            label = confirmLabel,
            onClick = { onConfirm(trimmedValue) },
            enabled = trimmedValue.isNotEmpty() && !duplicate,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        TextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = stringResource(MR.strings.name),
            placeholder = suggestion.ifEmpty { null },
            supportingText = if (showDuplicate) stringResource(MR.strings.profiles_name_exists) else null,
            isError = showDuplicate,
        )
    }
}
