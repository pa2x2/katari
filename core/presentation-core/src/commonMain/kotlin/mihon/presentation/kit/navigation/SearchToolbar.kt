package mihon.presentation.kit.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Search
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.feedback.Tooltip
import mihon.presentation.kit.field.SearchFieldFrame
import mihon.presentation.kit.field.rememberQueryTextFieldState
import mihon.presentation.kit.field.searchFieldTextStyle
import mihon.presentation.kit.shortcut.KeyboardShortcutHandler
import mihon.presentation.kit.shortcut.ScreenShortcuts
import mihon.presentation.kit.shortcut.typingFocus
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
 * up button while searching, for a screen that is only a search and has nowhere to go back to. [matchCounter], such as
 * "1 of 2", shows in the field for a search that steps through matches; with [onStepMatch] given, Enter steps to the
 * next match and Shift+Enter to the previous one, and the field keeps the cursor for the query to be changed. [overlay]
 * draws the bar over content, as a reader's bar is.
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
    matchCounter: String? = null,
    onStepMatch: ((forward: Boolean) -> Unit)? = null,
    overlay: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    // With the field already open, the shortcut puts the cursor back in it
    KeyboardShortcutHandler(ScreenShortcuts.Search, enabled = searchEnabled || searchQuery != null) {
        if (searchQuery == null) onChangeSearchQuery("") else focusRequester.requestFocus()
    }

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
                    matchCounter = matchCounter,
                    onStepMatch = onStepMatch,
                )
            }
        },
        navigateUp = if (searchQuery == null) navigateUp else onClickCloseSearch,
        actions = {
            key("search") {
                if (searchEnabled && searchQuery == null) {
                    val label = stringResource(MR.strings.action_search)
                    Tooltip(label, shortcut = ScreenShortcuts.Search) {
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
        overlay = overlay,
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
    matchCounter: String?,
    onStepMatch: ((forward: Boolean) -> Unit)?,
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

    val state = rememberQueryTextFieldState(searchQuery, onChangeSearchQuery)

    BasicTextField(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .typingFocus()
            .then(
                if (onStepMatch != null) {
                    Modifier.onPreviewKeyEvent { event ->
                        if (event.key != Key.Enter && event.key != Key.NumPadEnter) return@onPreviewKeyEvent false
                        if (event.type == KeyEventType.KeyDown) onStepMatch(!event.isShiftPressed)
                        true
                    }
                } else {
                    Modifier.runOnEnterKeyPressed(action = searchAndClearFocus)
                },
            )
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
                counter = matchCounter,
                innerTextField = innerTextField,
            )
        },
    )
}
