package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.roundedfilled.Error
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.TextField
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun EditTextPreferenceWidget(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    isProfileSpecific: Boolean,
    value: String,
    onConfirm: suspend (String) -> Boolean,
) {
    var isDialogShown by remember { mutableStateOf(false) }

    TextPreferenceWidget(
        title = title,
        subtitle = subtitle?.format(value),
        icon = icon,
        isProfileSpecific = isProfileSpecific,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        val scope = rememberCoroutineScope()
        val onDismissRequest = { isDialogShown = false }
        var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(value))
        }
        Dialog(
            onDismissRequest = onDismissRequest,
            title = title,
            confirm = DialogAction(
                label = stringResource(MR.strings.action_save),
                enabled = textFieldValue.text != value && textFieldValue.text.isNotBlank(),
                onClick = {
                    scope.launch {
                        if (onConfirm(textFieldValue.text)) {
                            onDismissRequest()
                        }
                    }
                },
            ),
            dismiss = DialogAction(stringResource(MR.strings.action_cancel), onClick = onDismissRequest),
        ) {
            TextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                trailingIcon = {
                    if (textFieldValue.text.isBlank()) {
                        Icon(imageVector = MaterialSymbols.RoundedFilled.Error, contentDescription = null)
                    } else {
                        IconButton(
                            icon = MaterialSymbols.Rounded.Close,
                            contentDescription = stringResource(MR.strings.action_reset),
                            onClick = { textFieldValue = TextFieldValue("") },
                        )
                    }
                },
                isError = textFieldValue.text.isBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
