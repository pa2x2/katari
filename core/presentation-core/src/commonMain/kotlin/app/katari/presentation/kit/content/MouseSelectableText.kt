package app.katari.presentation.kit.content

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import app.katari.presentation.kit.pointer.LocalWindowHasMouse

/**
 * Lets the text in [content], such as an error's details, be selected with the mouse and copied with Ctrl+C in a window
 * worked with a mouse. Touch windows leave it as plain text, so a long press there keeps doing what it did. [content]
 * is one text, since the container stacks what it holds.
 */
@Composable
internal fun MouseSelectableText(content: @Composable () -> Unit) {
    if (LocalWindowHasMouse.current) SelectionContainer(content = content) else content()
}
