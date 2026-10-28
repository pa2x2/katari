package mihon.presentation.kit.shortcut

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The shortcuts to a screen's own actions. The kit's search bar and pull to refresh declare theirs, and an app bar
 * action that is the screen's search names [Search] as its shortcut.
 */
object ScreenShortcuts {
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
}
