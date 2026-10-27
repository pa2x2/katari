package mihon.presentation.kit.field

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.LocalDialogConfirm
import mihon.presentation.kit.list.LocalInDialog

/**
 * The app's text field: outlined, with an optional [label], [placeholder] and [supportingText] that turns into the
 * error message when [isError]. In a dialog it spans the dialog, and a single-line field's Done key runs the dialog's
 * confirm action while it is enabled, unless the caller sets its own keyboard action.
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: String? = null,
    suffix: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource? = null,
) {
    val dialog = dialogFieldBehaviour(singleLine, keyboardOptions, keyboardActions)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.then(dialog.modifier),
        enabled = enabled,
        readOnly = readOnly,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = trailingIcon,
        prefix = prefix?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        keyboardOptions = dialog.keyboardOptions,
        keyboardActions = dialog.keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
    )
}

@Composable
fun TextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val dialog = dialogFieldBehaviour(singleLine, keyboardOptions, keyboardActions)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.then(dialog.modifier),
        enabled = enabled,
        readOnly = readOnly,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = trailingIcon,
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        keyboardOptions = dialog.keyboardOptions,
        keyboardActions = dialog.keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
    )
}

@Composable
fun TextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val dialog = dialogFieldBehaviour(singleLine, keyboardOptions, KeyboardActions.Default)
    OutlinedTextField(
        state = state,
        modifier = modifier.then(dialog.modifier),
        enabled = enabled,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        trailingIcon = trailingIcon,
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        keyboardOptions = dialog.keyboardOptions,
        onKeyboardAction = dialog.keyboardActionHandler,
        lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.Default,
    )
}

/**
 * A password or secret field that hides what is typed. [revealed] shows it instead, typically toggled by an icon
 * button in [trailingIcon].
 */
@Composable
fun SecureTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    revealed: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val dialog = dialogFieldBehaviour(singleLine = true, keyboardOptions, KeyboardActions.Default)
    OutlinedSecureTextField(
        state = state,
        modifier = modifier.then(dialog.modifier),
        enabled = enabled,
        label = label?.let { { Text(it) } },
        trailingIcon = trailingIcon,
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        textObfuscationMode = if (revealed) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
        keyboardOptions = dialog.keyboardOptions,
        onKeyboardAction = dialog.keyboardActionHandler,
    )
}

private class DialogFieldBehaviour(
    val modifier: Modifier,
    val keyboardOptions: KeyboardOptions,
    callerActions: KeyboardActions,
    confirm: (() -> Unit)?,
) {
    val keyboardActions: KeyboardActions = confirm?.let { KeyboardActions(onDone = { it() }) } ?: callerActions
    val keyboardActionHandler: KeyboardActionHandler? = confirm?.let { KeyboardActionHandler { it() } }
}

@Composable
private fun dialogFieldBehaviour(
    singleLine: Boolean,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
): DialogFieldBehaviour {
    if (!LocalInDialog.current) return DialogFieldBehaviour(Modifier, keyboardOptions, keyboardActions, confirm = null)
    val confirm = LocalDialogConfirm.current
    val ownAction = keyboardActions != KeyboardActions.Default || keyboardOptions.imeAction != ImeAction.Unspecified
    return if (confirm == null || !singleLine || ownAction) {
        DialogFieldBehaviour(Modifier.fillMaxWidth(), keyboardOptions, keyboardActions, confirm = null)
    } else {
        DialogFieldBehaviour(
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Done),
            callerActions = keyboardActions,
            confirm = { if (confirm.enabled) confirm.onClick() },
        )
    }
}
