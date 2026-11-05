package mihon.entry.viewer.chrome.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.LocalAwtWindow
import kotlinx.coroutines.delay
import mihon.core.platform.currentPlatformContext
import mihon.core.platform.power.ScreenIdleInhibitor
import java.awt.Frame
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowStateListener

/**
 * Keeps the screen awake while a viewer is [active], such as a video playing, in the window this is composed in, while
 * that window is shown, or [evenWhenHidden], as when a video plays in a window of its own. A minimised or hidden window
 * lets the screen lock, as nobody is looking at it, even while a video's sound plays on. The screen is let go of only
 * once that has lasted a moment, as switching to full screen hides the window for an instant and an episode change
 * stops playing for one. [reason] is what the desktop lists as keeping the screen awake.
 */
@Composable
fun KeepScreenAwake(
    active: Boolean,
    reason: String,
    inhibitor: ScreenIdleInhibitor,
    evenWhenHidden: Boolean = false,
) {
    val window = LocalAwtWindow.current ?: return
    val context = currentPlatformContext()
    val shown by rememberWindowShown(window)
    val keepsAwake = active && (shown || evenWhenHidden)
    val hold = remember { ScreenHold() }
    LaunchedEffect(keepsAwake) {
        if (keepsAwake) {
            if (hold.current == null) hold.current = inhibitor.keepAwake(context, reason)
        } else {
            delay(RELEASE_DELAY_MS)
            hold.release()
        }
    }
    DisposableEffect(hold) {
        onDispose(hold::release)
    }
}

private class ScreenHold {
    var current: AutoCloseable? = null

    fun release() {
        current?.close()
        current = null
    }
}

private const val RELEASE_DELAY_MS = 1_500L

@Composable
private fun rememberWindowShown(window: Window) = remember(window) { mutableStateOf(window.isShownOnScreen) }
    .also { shown ->
        DisposableEffect(window) {
            val stateListener = WindowStateListener { _: WindowEvent -> shown.value = window.isShownOnScreen }
            val componentListener = object : ComponentAdapter() {
                override fun componentShown(event: ComponentEvent) {
                    shown.value = window.isShownOnScreen
                }

                override fun componentHidden(event: ComponentEvent) {
                    shown.value = window.isShownOnScreen
                }
            }
            window.addWindowStateListener(stateListener)
            window.addComponentListener(componentListener)
            onDispose {
                window.removeWindowStateListener(stateListener)
                window.removeComponentListener(componentListener)
            }
        }
    }

private val Window.isShownOnScreen: Boolean
    get() = isShowing && (this !is Frame || extendedState and Frame.ICONIFIED == 0)
