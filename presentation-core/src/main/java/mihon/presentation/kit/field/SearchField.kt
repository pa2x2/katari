package mihon.presentation.kit.field

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A search field for filtering a list or picker, drawn like the search field of a SearchToolbar. */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    val state = rememberTextFieldState(query)
    val currentQuery by rememberUpdatedState(query)
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    LaunchedEffect(query) {
        if (state.text.toString() != query) state.setTextAndPlaceCursorAtEnd(query)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect {
            if (it != currentQuery) currentOnQueryChange(it)
        }
    }
    SearchField(state = state, placeholder = placeholder, modifier = modifier, onSearch = onSearch)
}

/**
 * A search field over a [state] the caller owns, for a query that is only run on demand, such as a search screen's
 * query field in its app bar. The keyboard's search key runs [onSearch].
 */
@Composable
fun SearchField(
    state: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    BasicTextField(
        state = state,
        modifier = modifier,
        textStyle = searchFieldTextStyle(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { onSearch?.invoke() },
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(KatariTheme.colors.onSurface),
        decorator = { innerTextField ->
            SearchFieldFrame(
                query = state.text.toString(),
                placeholder = placeholder,
                clearLabel = stringResource(MR.strings.action_clear),
                onClear = { state.clearText() },
                innerTextField = innerTextField,
            )
        },
    )
}
