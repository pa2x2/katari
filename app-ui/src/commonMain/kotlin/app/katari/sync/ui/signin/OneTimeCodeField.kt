package app.katari.sync.ui.signin

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import app.katari.presentation.kit.field.TextField

/**
 * A field for a code the server made, such as a link or invite code: the server shows them in groups of four joined
 * by dashes and reads them back ignoring case and dashes. The field holds only the code's letters and digits, upper
 * case, and shows the dashes between groups itself, so typing or pasting a code in any form reads the way it was shown.
 */
@Composable
internal fun OneTimeCodeField(
    state: TextFieldState,
    label: String,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    TextField(
        state = state,
        modifier = modifier,
        label = label,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            imeAction = imeAction,
        ),
        inputTransformation = KeepCodeCharacters,
        outputTransformation = DashBetweenGroups,
    )
}

/** [code] as [OneTimeCodeField] holds it, for filling the field in with a code that didn't come from typing. */
internal fun oneTimeCodeText(code: String): String = code.mapNotNull { it.codeCharacter() }.joinToString("")

private fun Char.codeCharacter(): Char? = uppercaseChar().takeIf { it in 'A'..'Z' || it in '0'..'9' }

// Edited character by character, not replaced whole, so the cursor stays where the person is typing
private val KeepCodeCharacters = InputTransformation {
    for (index in length - 1 downTo 0) {
        val typed = charAt(index)
        val kept = typed.codeCharacter()
        when {
            kept == null -> delete(index, index + 1)
            kept != typed -> replace(index, index + 1, kept.toString())
        }
    }
}

private val DashBetweenGroups = OutputTransformation {
    for (index in (length - 1) / GROUP_LENGTH * GROUP_LENGTH downTo GROUP_LENGTH step GROUP_LENGTH) {
        insert(index, "-")
    }
}

private const val GROUP_LENGTH = 4
