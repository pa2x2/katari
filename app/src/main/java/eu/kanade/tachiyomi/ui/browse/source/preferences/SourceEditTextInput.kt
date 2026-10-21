package eu.kanade.tachiyomi.ui.browse.source.preferences

import android.content.Context
import android.text.InputType
import android.widget.EditText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.preference.EditTextPreference
import androidx.preference.getOnBindEditTextListener

/**
 * How a source's text preference wants to be edited. Sources describe that by configuring the dialog's [EditText]
 * in `setOnBindEditTextListener` (input type, starting text, hint), so the listener runs against a detached one,
 * as androidx's dialog would run it, and the result is read back for the Compose field.
 */
internal data class SourceEditTextInput(
    val text: String,
    val hint: String?,
    val secret: Boolean,
    val singleLine: Boolean,
    val keyboardOptions: KeyboardOptions,
)

internal fun EditTextPreference.editTextInput(context: Context): SourceEditTextInput {
    val editText = EditText(context)
    val current = text.orEmpty()
    editText.setText(current)
    editText.setSelection(current.length)
    getOnBindEditTextListener()?.onBindEditText(editText)
    return editText.toInput()
}

private fun EditText.toInput(): SourceEditTextInput {
    val type = inputType
    val inputClass = type and InputType.TYPE_MASK_CLASS
    val variation = type and InputType.TYPE_MASK_VARIATION
    val secret = when (inputClass) {
        InputType.TYPE_CLASS_TEXT ->
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else -> false
    }
    val keyboardType = when (inputClass) {
        InputType.TYPE_CLASS_NUMBER -> when {
            secret -> KeyboardType.NumberPassword
            type and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0 -> KeyboardType.Decimal
            else -> KeyboardType.Number
        }
        InputType.TYPE_CLASS_PHONE -> KeyboardType.Phone
        InputType.TYPE_CLASS_TEXT -> when (variation) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            -> KeyboardType.Password
            InputType.TYPE_TEXT_VARIATION_URI -> KeyboardType.Uri
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            -> KeyboardType.Email
            else -> KeyboardType.Text
        }
        else -> KeyboardType.Text
    }
    val capitalization = when {
        type and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS != 0 -> KeyboardCapitalization.Characters
        type and InputType.TYPE_TEXT_FLAG_CAP_WORDS != 0 -> KeyboardCapitalization.Words
        type and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0 -> KeyboardCapitalization.Sentences
        else -> KeyboardCapitalization.None
    }
    // A plain EditText is multi-line, as androidx's dialog field is unless the source picks another input type.
    val singleLine = inputClass != InputType.TYPE_CLASS_TEXT || type and InputType.TYPE_TEXT_FLAG_MULTI_LINE == 0
    return SourceEditTextInput(
        text = text.toString(),
        hint = hint?.toString()?.takeIf { it.isNotBlank() },
        secret = secret,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            autoCorrectEnabled = type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS == 0 && !secret,
            keyboardType = keyboardType,
            imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
        ),
    )
}
