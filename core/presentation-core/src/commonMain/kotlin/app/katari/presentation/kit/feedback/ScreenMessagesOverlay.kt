package app.katari.presentation.kit.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Draws the [ScreenMessages] of a screen that fills the window with no Scaffold, such as a reader over its pages, at
 * the bottom of the screen. Without it they would fall through to [AppMessages] and wait for a screen that shows
 * messages, which is the one under it once it closes. Where no screen gives messages, as in an Android activity, they
 * stay toasts and this draws nothing.
 */
@Composable
fun ScreenMessagesOverlay(modifier: Modifier = Modifier) {
    val messages = claimScreenMessages() ?: return
    Box(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ScreenSnackbarHost(own = null, messages = messages)
    }
}
