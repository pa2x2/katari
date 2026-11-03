package mihon.desktop.display.size

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import mihon.presentation.kit.shortcut.KeyboardShortcut
import mihon.presentation.kit.shortcut.KeyboardShortcutHandler
import mihon.presentation.kit.shortcut.ShortcutGroup
import mihon.presentation.kit.shortcut.ShortcutKeys
import mihon.presentation.kit.shortcut.ShortcutReach
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Ctrl+= and Ctrl+− step the interface size up and down and Ctrl+0 resets it, as browsers zoom, the keypad's keys and
 * Ctrl+Shift+= ("+") too.
 */
@Composable
fun InterfaceSizeShortcuts(preferences: InterfaceSizePreferences) {
    val size = preferences.interfaceSize
    KeyboardShortcutHandler(
        KeyboardShortcut(
            id = "window.interfaceSize.larger",
            label = stringResource(MR.strings.keyboard_shortcut_interface_larger),
            keys = listOf(
                ShortcutKeys.Chord(Key.Equals, "=", ctrl = true),
                ShortcutKeys.Chord(Key.Equals, "+", ctrl = true, shift = true),
                ShortcutKeys.Chord(Key.NumPadAdd, "+", ctrl = true),
            ),
            group = ShortcutGroup.Window,
            reach = ShortcutReach.Window,
        ),
    ) { size.set(InterfaceSize.larger(size.get())) }
    KeyboardShortcutHandler(
        KeyboardShortcut(
            id = "window.interfaceSize.smaller",
            label = stringResource(MR.strings.keyboard_shortcut_interface_smaller),
            keys = listOf(
                ShortcutKeys.Chord(Key.Minus, "−", ctrl = true),
                ShortcutKeys.Chord(Key.NumPadSubtract, "−", ctrl = true),
            ),
            group = ShortcutGroup.Window,
            reach = ShortcutReach.Window,
        ),
    ) { size.set(InterfaceSize.smaller(size.get())) }
    KeyboardShortcutHandler(
        KeyboardShortcut(
            id = "window.interfaceSize.reset",
            label = stringResource(MR.strings.keyboard_shortcut_interface_reset),
            keys = listOf(
                ShortcutKeys.Chord(Key.Zero, "0", ctrl = true),
                ShortcutKeys.Chord(Key.NumPad0, "0", ctrl = true),
            ),
            group = ShortcutGroup.Window,
            reach = ShortcutReach.Window,
        ),
    ) { size.set(InterfaceSize.DEFAULT) }
}
