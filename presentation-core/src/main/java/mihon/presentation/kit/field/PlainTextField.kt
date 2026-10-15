package mihon.presentation.kit.field

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.theme.style

/**
 * A multi-line text area without a frame, for editors whose container already marks the input, such as the
 * translator's source pane. It shows between [minLines] and [maxLines] lines of [role] text and scrolls past them.
 * With [onSearch] it is a one-line query instead, run by the keyboard's search key, such as a search in an app bar.
 */
@Composable
fun PlainTextField(
    state: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
    role: TextRole = TextRole.ItemTitle,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    onSearch: (() -> Unit)? = null,
) {
    val colors = KatariTheme.colors
    BasicTextField(
        state = state,
        modifier = modifier,
        textStyle = role.style(MaterialTheme.typography).copy(color = colors.onSurface),
        lineLimits = if (onSearch != null) {
            TextFieldLineLimits.SingleLine
        } else {
            TextFieldLineLimits.MultiLine(minHeightInLines = minLines, maxHeightInLines = maxLines)
        },
        keyboardOptions = if (onSearch !=
            null
        ) {
            KeyboardOptions(imeAction = ImeAction.Search)
        } else {
            KeyboardOptions.Default
        },
        onKeyboardAction = onSearch?.let { search -> KeyboardActionHandler { search() } },
        cursorBrush = SolidColor(colors.primary),
        decorator = { innerTextField ->
            Box {
                if (state.text.isEmpty()) {
                    Text(placeholder, role = role, tone = ContentTone.Secondary)
                }
                innerTextField()
            }
        },
    )
}
