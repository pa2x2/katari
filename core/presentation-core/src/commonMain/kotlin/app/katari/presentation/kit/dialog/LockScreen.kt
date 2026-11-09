package app.katari.presentation.kit.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.katari.presentation.kit.layout.ApartFromScreen
import app.katari.presentation.kit.navigation.DockClearance
import app.katari.presentation.kit.navigation.LocalDockOnScreen
import app.katari.presentation.kit.shortcut.KeyboardShortcutLayer

/**
 * Covers the whole window until it is removed, such as the profile picker over a profile that locked again while the
 * app was away: the screens under it stay as they were but get no input, focus or shortcuts, and back or a click
 * doesn't dismiss it. [content] fills the window edge to edge and draws its own background.
 */
@Composable
fun LockScreen(content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties = kitDialogProperties(
            base = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            fitsSystemWindows = false,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ApartFromScreen {
                CompositionLocalProvider(LocalDockOnScreen provides DockClearance.None) {
                    KeyboardShortcutLayer(content)
                }
            }
        }
    }
}
