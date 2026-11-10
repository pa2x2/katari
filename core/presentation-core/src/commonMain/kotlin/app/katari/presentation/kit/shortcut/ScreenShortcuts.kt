package app.katari.presentation.kit.shortcut

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The shortcuts to a screen's own actions. The kit's search bar, refresh button and selectable items declare theirs,
 * and an app bar action that is the screen's search names [Search] as its shortcut.
 */
object ScreenShortcuts {
    /**
     * ? beside the window's Ctrl+/, on a screen whose keys are single letters, such as the player's and the readers':
     * it opens the list of shortcuts ([KeyboardShortcutRegistry.toggleList]), and reaches the list to close it again.
     */
    val ShortcutList: KeyboardShortcut
        @Composable get() = KeyboardShortcut(
            id = "screen.shortcuts",
            label = stringResource(MR.strings.keyboard_shortcut_list),
            keys = listOf(ShortcutKeys.Chord(Key.Slash, "?", shift = true)),
            group = ShortcutGroup.CurrentScreen,
            reach = ShortcutReach.Window,
            whileTyping = false,
            beforeFocus = true,
        )

    val Search: KeyboardShortcut
        @Composable get() = KeyboardShortcut(
            id = "screen.search",
            label = stringResource(MR.strings.keyboard_shortcut_search),
            keys = listOf(ShortcutKeys.Chord(Key.F, "F", ctrl = true)),
            group = ShortcutGroup.CurrentScreen,
            whileTyping = false,
        )

    val Refresh: KeyboardShortcut
        @Composable get() = KeyboardShortcut(
            id = "screen.refresh",
            label = stringResource(MR.strings.action_refresh),
            keys = listOf(ShortcutKeys.Chord(Key.F5, "F5")),
            group = ShortcutGroup.CurrentScreen,
        )

    /** Leaves Ctrl+A to a focused text field, where it selects the field's text. */
    val SelectAll: KeyboardShortcut
        @Composable get() = KeyboardShortcut(
            id = "screen.selectAll",
            label = stringResource(MR.strings.action_select_all),
            keys = listOf(ShortcutKeys.Chord(Key.A, "A", ctrl = true)),
            group = ShortcutGroup.CurrentScreen,
            whileTyping = false,
        )

    /** Declared after the window's back, so Esc clears a selection before it goes back. */
    val ClearSelection: KeyboardShortcut
        @Composable get() = KeyboardShortcut(
            id = "screen.clearSelection",
            label = stringResource(MR.strings.keyboard_shortcut_clear_selection),
            keys = listOf(ShortcutKeys.Chord(Key.Escape, stringResource(MR.strings.key_escape))),
            group = ShortcutGroup.CurrentScreen,
        )
}
