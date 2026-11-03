package mihon.presentation.kit.shortcut

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged

/**
 * Tells the window's shortcuts that a text field is being typed in while the field after this has focus, so shortcuts
 * on keys typing may use leave them to it (see [KeyboardShortcut.whileTyping]). A text field doesn't consume keys it
 * has no command for, such as Ctrl+/, so they would otherwise reach the window's shortcuts.
 */
@Composable
internal fun Modifier.typingFocus(): Modifier {
    val registry = LocalKeyboardShortcuts.current ?: return this
    val field = remember(registry) { Any() }
    // A field removed while focused reports no focus change
    DisposableEffect(field) { onDispose { registry.typingEnded(field) } }
    return onFocusChanged { if (it.hasFocus) registry.typingStarted(field) else registry.typingEnded(field) }
}
