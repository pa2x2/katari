package eu.kanade.tachiyomi.ui.browse.source.browse.filter.control

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

internal fun Modifier.filterField(): Modifier = fillMaxWidth()
    .padding(horizontal = FilterSheetInsets.Horizontal, vertical = FilterSheetInsets.FieldVertical)

/** A free-form text filter. */
@Composable
internal fun FilterTextField(label: String, value: String, isError: Boolean, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.filterField(),
        label = { Text(text = label) },
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        isError = isError,
        trailingIcon = { FilterFieldClearButton(label, visible = value.isNotEmpty()) { onValueChange("") } },
        keyboardOptions = FilterFieldKeyboardOptions,
        keyboardActions = filterFieldKeyboardActions(),
    )
}

/** Text filters move on to the field below; the last one closes the keyboard instead of wrapping to the top. */
internal val FilterFieldKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)

@Composable
internal fun filterFieldKeyboardActions(): KeyboardActions {
    val focusManager = LocalFocusManager.current
    return KeyboardActions(onNext = { if (!focusManager.moveFocus(FocusDirection.Down)) focusManager.clearFocus() })
}

@Composable
internal fun FilterFieldClearButton(label: String, visible: Boolean, onClear: () -> Unit) {
    if (!visible) return
    IconButton(onClick = onClear) {
        Icon(Icons.Outlined.Clear, contentDescription = stringResource(MR.strings.filter_clear_value, label))
    }
}
