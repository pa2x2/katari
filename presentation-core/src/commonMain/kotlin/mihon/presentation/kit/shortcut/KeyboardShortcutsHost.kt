package mihon.presentation.kit.shortcut

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Gives [content] the window's keyboard shortcuts in [registry], and shows their list on Ctrl+/ or
 * [KeyboardShortcutRegistry.showList]. The window hands its key events to [registry].
 */
@Composable
fun KeyboardShortcutsHost(registry: KeyboardShortcutRegistry, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalKeyboardShortcuts provides registry) {
        KeyboardShortcutHandler(showListShortcut()) {
            if (registry.listed == null) registry.showList() else registry.hideList()
        }
        content()
        val listed = registry.listed
        if (listed != null) {
            KeyboardShortcutsSheet(shortcuts = listed + openFocusedShortcut(), onDismissRequest = registry::hideList)
        }
    }
}

@Composable
private fun showListShortcut() = KeyboardShortcut(
    id = "window.shortcuts",
    label = stringResource(MR.strings.keyboard_shortcut_list),
    keys = listOf(ShortcutKeys.Chord(Key.Slash, "/", ctrl = true)),
    group = ShortcutGroup.Window,
    reach = ShortcutReach.Window,
    whileTyping = false,
)

/**
 * Listed only: a focused button, row or cover already opens on Enter by itself. Ctrl+Enter opens it the same way,
 * since clicks from the keyboard ignore modifiers, and is left out until something opens in a window of its own.
 */
@Composable
private fun openFocusedShortcut() = KeyboardShortcut(
    id = "navigation.open",
    label = stringResource(MR.strings.keyboard_shortcut_open_focused),
    keys = listOf(ShortcutKeys.Chord(Key.Enter, stringResource(MR.strings.key_enter))),
    group = ShortcutGroup.Navigation,
)
