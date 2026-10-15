package eu.kanade.tachiyomi.ui.browse.source.browse.filter.control

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.field.TextField
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Text fields line up with the sheet's rows. */
private fun Modifier.filterField(): Modifier = fillMaxWidth()
    .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.s)

/** A free-form text filter. */
@Composable
internal fun FilterTextField(label: String, value: String, isError: Boolean, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.filterField(),
        label = label,
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
    IconButton(
        icon = MaterialSymbols.Rounded.Close,
        contentDescription = stringResource(MR.strings.filter_clear_value, label),
        onClick = onClear,
    )
}
