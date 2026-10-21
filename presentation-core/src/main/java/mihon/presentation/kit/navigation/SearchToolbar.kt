package mihon.presentation.kit.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Search
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.feedback.Tooltip
import mihon.presentation.kit.field.SearchFieldFrame
import mihon.presentation.kit.field.searchFieldTextStyle
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clearFocusOnSoftKeyboardHide
import tachiyomi.presentation.core.util.runOnEnterKeyPressed
import tachiyomi.presentation.core.util.showSoftKeyboard

/**
 * An app bar that turns into a search field. [searchQuery] null shows the normal bar with a search action; any string
 * shows the field, and up then closes the search. [searchEnabled] false hides the search action.
 * [placeholderText] null uses [MR.strings.action_search_hint]. While searching the bar is always compact and holds
 * the query in a rounded field, so it reads as a search box rather than a title. [onClickCloseSearch] null hides the
 * up button while searching, for a screen that is only a search and has nowhere to go back to.
 */
@Composable
fun SearchToolbar(
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    modifier: Modifier = Modifier,
    titleContent: @Composable () -> Unit = {},
    navigateUp: (() -> Unit)? = null,
    searchEnabled: Boolean = true,
    placeholderText: String? = null,
    onSearch: (String) -> Unit = {},
    onClickCloseSearch: (() -> Unit)? = { onChangeSearchQuery(null) },
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: AppBarScrollBehavior? = null,
    large: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }

    AppBar(
        modifier = modifier,
        titleContent = {
            if (searchQuery == null) {
                titleContent()
            } else {
                SearchField(
                    searchQuery = searchQuery,
                    onChangeSearchQuery = onChangeSearchQuery,
                    onSearch = onSearch,
                    placeholderText = placeholderText ?: stringResource(MR.strings.action_search_hint),
                    focusRequester = focusRequester,
                )
            }
        },
        navigateUp = if (searchQuery == null) navigateUp else onClickCloseSearch,
        actions = {
            key("search") {
                if (searchEnabled && searchQuery == null) {
                    val label = stringResource(MR.strings.action_search)
                    Tooltip(label) {
                        IconButton(
                            icon = MaterialSymbols.Rounded.Search,
                            contentDescription = label,
                            onClick = { onChangeSearchQuery("") },
                        )
                    }
                }
            }
            key("actions") { actions() }
        },
        scrollBehavior = scrollBehavior,
        large = large && searchQuery == null,
    )
}

@Composable
private fun SearchField(
    searchQuery: String,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    placeholderText: String,
    focusRequester: FocusRequester,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val searchAndClearFocus: () -> Unit = f@{
        if (searchQuery.isBlank()) return@f
        onSearch(searchQuery)
        focusManager.clearFocus()
        keyboardController?.hide()
        focusManager.moveFocus(FocusDirection.Next)
    }

    val state = rememberTextFieldState(searchQuery)
    val currentQuery by rememberUpdatedState(searchQuery)
    val currentOnChangeSearchQuery by rememberUpdatedState(onChangeSearchQuery)
    LaunchedEffect(searchQuery) {
        if (state.text.toString() != searchQuery) {
            state.edit { replace(0, length, searchQuery) }
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect {
            if (it != currentQuery) currentOnChangeSearchQuery(it)
        }
    }

    BasicTextField(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .runOnEnterKeyPressed(action = searchAndClearFocus)
            .showSoftKeyboard(remember { searchQuery.isEmpty() })
            .clearFocusOnSoftKeyboardHide(),
        textStyle = searchFieldTextStyle(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { searchAndClearFocus() },
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(KatariTheme.colors.onSurface),
        decorator = { innerTextField ->
            SearchFieldFrame(
                query = searchQuery,
                placeholder = placeholderText,
                clearLabel = stringResource(MR.strings.action_reset),
                onClear = {
                    onChangeSearchQuery("")
                    focusRequester.requestFocus()
                },
                modifier = Modifier.padding(end = KatariTheme.spacing.s),
                innerTextField = innerTextField,
            )
        },
    )
}
