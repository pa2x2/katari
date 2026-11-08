package eu.kanade.tachiyomi.ui.browse.source.preferences

import android.content.Context
import android.text.InputType
import android.widget.EditText
import androidx.preference.EditTextPreference
import androidx.preference.getOnBindEditTextListener

/**
 * How a source's text preference wants to be edited. Sources describe that by configuring the dialog's [EditText]
 * in `setOnBindEditTextListener` (input type, starting text, hint), so the listener runs against a detached one,
 * as androidx's dialog would run it, and the result is read back for the app's own field.
 */
data class SourceEditTextInput(
    val text: String,
    val hint: String?,
    val secret: Boolean,
    val singleLine: Boolean,
    val keyboard: SourceTextKeyboard,
    val capitalization: SourceTextCapitalization,
    val autoCorrect: Boolean,
)

/** The keyboard an [InputType] asks for. */
enum class SourceTextKeyboard {
    Text,
    Number,
    Decimal,
    NumberPassword,
    Phone,
    Password,
    Uri,
    Email,
}

enum class SourceTextCapitalization {
    None,
    Characters,
    Words,
    Sentences,
}

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
    val keyboard = when (inputClass) {
        InputType.TYPE_CLASS_NUMBER -> when {
            secret -> SourceTextKeyboard.NumberPassword
            type and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0 -> SourceTextKeyboard.Decimal
            else -> SourceTextKeyboard.Number
        }
        InputType.TYPE_CLASS_PHONE -> SourceTextKeyboard.Phone
        InputType.TYPE_CLASS_TEXT -> when (variation) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            -> SourceTextKeyboard.Password
            InputType.TYPE_TEXT_VARIATION_URI -> SourceTextKeyboard.Uri
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            -> SourceTextKeyboard.Email
            else -> SourceTextKeyboard.Text
        }
        else -> SourceTextKeyboard.Text
    }
    val capitalization = when {
        type and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS != 0 -> SourceTextCapitalization.Characters
        type and InputType.TYPE_TEXT_FLAG_CAP_WORDS != 0 -> SourceTextCapitalization.Words
        type and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0 -> SourceTextCapitalization.Sentences
        else -> SourceTextCapitalization.None
    }
    // A plain EditText is multi-line, as androidx's dialog field is unless the source picks another input type.
    val singleLine = inputClass != InputType.TYPE_CLASS_TEXT || type and InputType.TYPE_TEXT_FLAG_MULTI_LINE == 0
    return SourceEditTextInput(
        text = text.toString(),
        hint = hint?.toString()?.takeIf { it.isNotBlank() },
        secret = secret,
        singleLine = singleLine,
        keyboard = keyboard,
        capitalization = capitalization,
        autoCorrect = type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS == 0 && !secret,
    )
}
