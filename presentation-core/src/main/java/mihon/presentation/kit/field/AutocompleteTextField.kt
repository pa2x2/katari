package mihon.presentation.kit.field

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import mihon.presentation.kit.content.Text

/**
 * A text field with a menu of suggestions under it, such as values a source offers for a filter. The caller decides
 * when the menu shows through [expanded]; [onExpandedChange] reports the user asking to open it (tapping the field) or
 * to close it (tapping outside). [modifier] applies to the field, which the menu matches in width. Fill
 * [suggestions] with DropdownMenuItem.
 */
@Composable
fun AutocompleteTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    isError: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    suggestions: @Composable ColumnScope.() -> Unit,
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .then(modifier),
            label = label?.let { { Text(it) } },
            isError = isError,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.exposedDropdownSize(matchAnchorWidth = true),
            content = suggestions,
        )
    }
}
