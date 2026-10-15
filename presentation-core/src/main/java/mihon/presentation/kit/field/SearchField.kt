package mihon.presentation.kit.field

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Search
import mihon.presentation.kit.button.IconButton
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A search field for filtering a list or picker; shows a clear button while [query] is not empty. */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = MaterialSymbols.Rounded.Search,
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(
                    icon = MaterialSymbols.Rounded.Close,
                    contentDescription = stringResource(MR.strings.action_clear),
                    onClick = { onQueryChange("") },
                )
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
    )
}
