package mihon.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import mihon.presentation.kit.shortcut.KeyboardShortcut
import mihon.presentation.kit.shortcut.KeyboardShortcutHandler
import mihon.presentation.kit.shortcut.ShortcutGroup
import mihon.presentation.kit.shortcut.ShortcutKeys
import mihon.presentation.kit.shortcut.ShortcutReach
import mihon.presentation.kit.window.WindowFullScreen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The window's own shortcuts, whatever is open in it: back on Esc or Alt+←, as the mouse's back button goes, full screen
 * on F11, and quitting on Ctrl+Q. Navigation has no forward history to go to, so Alt+→ does nothing.
 */
@Composable
fun WindowShortcuts(fullScreen: WindowFullScreen, backInput: WindowBackInput, onQuit: () -> Unit) {
    // Compose goes back on an Esc nothing in the window used; taking it here first does the same
    KeyboardShortcutHandler(
        KeyboardShortcut(
            id = "navigation.back",
            label = stringResource(MR.strings.action_back),
            keys = listOf(
                ShortcutKeys.Chord(Key.Escape, stringResource(MR.strings.key_escape)),
                ShortcutKeys.Chord(Key.DirectionLeft, "←", alt = true),
                ShortcutKeys.MouseBack,
            ),
            group = ShortcutGroup.Navigation,
            reach = ShortcutReach.Window,
        ),
        onTrigger = backInput::goBack,
    )

    KeyboardShortcutHandler(
        KeyboardShortcut(
            id = "window.fullScreen",
            label = stringResource(MR.strings.keyboard_shortcut_full_screen),
            keys = listOf(ShortcutKeys.Chord(Key.F11, "F11")),
            group = ShortcutGroup.Window,
            reach = ShortcutReach.Window,
        ),
    ) {
        if (fullScreen.isFullScreen) fullScreen.leave() else fullScreen.enter()
    }

    KeyboardShortcutHandler(
        KeyboardShortcut(
            id = "window.quit",
            label = stringResource(MR.strings.keyboard_shortcut_quit),
            keys = listOf(ShortcutKeys.Chord(Key.Q, "Q", ctrl = true)),
            group = ShortcutGroup.Window,
            reach = ShortcutReach.Window,
        ),
        onTrigger = onQuit,
    )
}
