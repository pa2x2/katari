package mihon.presentation.kit.shortcut

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Gives [content], which fills the window, the window's keyboard shortcuts in [registry], and shows their list on
 * Ctrl+/ or [KeyboardShortcutRegistry.showList]. The window hands [registry] the key events its content leaves; the
 * keys on their way to a focused control in [content] reach it from here first. A window opened from content kept
 * from running its shortcuts ([KeyboardShortcutScope]) has its own running in it.
 */
@Composable
fun KeyboardShortcutsHost(registry: KeyboardShortcutRegistry, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalKeyboardShortcuts provides registry,
        LocalShortcutLayer provides null,
        LocalShortcutsActive provides true,
    ) {
        KeyboardShortcutHandler(showListShortcut()) {
            if (registry.listed == null) registry.showList() else registry.hideList()
        }
        Box(modifier = Modifier.fillMaxSize().onPreviewKeyEvent(registry::onPreviewKeyEvent)) { content() }
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
