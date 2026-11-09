package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import app.katari.entry.viewer.chrome.ui.window.ViewerFullScreen
import app.katari.presentation.kit.shortcut.KeyboardShortcut
import app.katari.presentation.kit.shortcut.KeyboardShortcutHandler
import app.katari.presentation.kit.shortcut.ShortcutGroup
import app.katari.presentation.kit.shortcut.ShortcutKeys
import app.katari.presentation.kit.window.LocalWindowFullScreen
import app.katari.presentation.kit.window.WindowFullScreen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The immersive view's full screen, as the readers and the player have it: F switches it beside the window's F11, Esc
 * leaves it before it leaves the immersive view, and leaving the immersive view puts the window back as it was. The
 * [content] gets the window's full screen to offer it, or null where the window has none, as on Android.
 */
@Composable
internal fun EntryImmersiveFullScreen(content: @Composable (WindowFullScreen?) -> Unit) {
    val fullScreen = LocalWindowFullScreen.current
    ViewerFullScreen(fullScreen) {
        if (fullScreen != null) {
            KeyboardShortcutHandler(
                KeyboardShortcut(
                    id = "immersive.fullScreen",
                    label = stringResource(MR.strings.keyboard_shortcut_full_screen),
                    keys = listOf(ShortcutKeys.Chord(Key.F, "F")),
                    group = ShortcutGroup.CurrentScreen,
                    whileTyping = false,
                    beforeFocus = true,
                ),
            ) { fullScreen.toggle() }
        }
        content(fullScreen)
    }
}

internal fun WindowFullScreen.toggle() {
    if (isFullScreen) leave() else enter()
}
