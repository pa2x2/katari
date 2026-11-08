package eu.kanade.tachiyomi.ui.browse.source.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Visibility
import app.katari.icons.materialsymbols.rounded.VisibilityOff
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.field.SecureTextField
import app.katari.presentation.kit.field.TextField
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Edits a source's text preference. Unlike the app's own text settings it accepts an empty value, since sources
 * use one to mean "not set", such as a cleared custom domain.
 */
@Composable
internal fun SourceEditTextDialog(
    title: String,
    message: String?,
    input: SourceEditTextInput,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val state = rememberTextFieldState(input.text)
    val focusRequester = remember { FocusRequester() }
    val keyboardOptions = remember(input) { input.keyboardOptions() }
    Dialog(
        onDismissRequest = onDismissRequest,
        title = title,
        confirm = DialogAction(
            label = stringResource(MR.strings.action_save),
            onClick = { onConfirm(state.text.toString()) },
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onClick = onDismissRequest),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
            if (message != null) {
                Text(text = message, role = TextRole.Body, tone = ContentTone.Secondary)
            }
            val fieldModifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
            if (input.secret) {
                var revealed by remember { mutableStateOf(false) }
                SecureTextField(
                    state = state,
                    modifier = fieldModifier,
                    label = input.hint,
                    revealed = revealed,
                    trailingIcon = {
                        IconButton(
                            icon = if (revealed) {
                                MaterialSymbols.Rounded.VisibilityOff
                            } else {
                                MaterialSymbols.Rounded.Visibility
                            },
                            contentDescription = stringResource(
                                if (revealed) MR.strings.action_hide_password else MR.strings.action_show_password,
                            ),
                            onClick = { revealed = !revealed },
                        )
                    },
                    keyboardOptions = keyboardOptions,
                )
            } else {
                TextField(
                    state = state,
                    modifier = fieldModifier,
                    placeholder = input.hint,
                    singleLine = input.singleLine,
                    keyboardOptions = keyboardOptions,
                )
            }
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

private fun SourceEditTextInput.keyboardOptions() = KeyboardOptions(
    capitalization = when (capitalization) {
        SourceTextCapitalization.None -> KeyboardCapitalization.None
        SourceTextCapitalization.Characters -> KeyboardCapitalization.Characters
        SourceTextCapitalization.Words -> KeyboardCapitalization.Words
        SourceTextCapitalization.Sentences -> KeyboardCapitalization.Sentences
    },
    autoCorrectEnabled = autoCorrect,
    keyboardType = when (keyboard) {
        SourceTextKeyboard.Text -> KeyboardType.Text
        SourceTextKeyboard.Number -> KeyboardType.Number
        SourceTextKeyboard.Decimal -> KeyboardType.Decimal
        SourceTextKeyboard.NumberPassword -> KeyboardType.NumberPassword
        SourceTextKeyboard.Phone -> KeyboardType.Phone
        SourceTextKeyboard.Password -> KeyboardType.Password
        SourceTextKeyboard.Uri -> KeyboardType.Uri
        SourceTextKeyboard.Email -> KeyboardType.Email
    },
    imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
)
