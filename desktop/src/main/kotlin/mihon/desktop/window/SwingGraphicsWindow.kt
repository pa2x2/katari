package mihon.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.AwtWindow
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.RenderSettings
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import mihon.presentation.kit.shortcut.KeyboardShortcutRegistry
import java.awt.Frame
import java.awt.KeyEventPostProcessor
import java.awt.KeyboardFocusManager
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.math.roundToInt
import java.awt.event.KeyEvent as AwtKeyEvent

/**
 * A Swing frame Compose draws into through Swing's graphics, for toolkits Compose's own window can't draw on. It
 * follows [state] as Compose's window does, both ways, and hands [shortcuts] the keys its content leaves, as Compose's
 * window does. Compose's frame lives in its own composition, so [content] gets the frame to build its window on.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SwingGraphicsWindow(
    state: WindowState,
    title: String,
    icon: Painter,
    onCloseRequest: () -> Unit,
    shortcuts: KeyboardShortcutRegistry,
    content: @Composable (JFrame) -> Unit,
) {
    val currentOnCloseRequest by rememberUpdatedState(onCloseRequest)
    val currentContent by rememberUpdatedState(content)
    val frame = remember {
        JFrame(title).apply {
            defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
            iconImage = icon.toAwtImage(Density(1f), LayoutDirection.Ltr, ICON_SIZE)
            addWindowListener(
                object : WindowAdapter() {
                    override fun windowClosing(event: WindowEvent) = currentOnCloseRequest()
                },
            )
            val panel = ComposePanel(renderSettings = RenderSettings.SwingGraphics())
            panel.setContent { currentContent(this@apply) }
            contentPane.add(panel)
            placeAt(state)
            followInto(state)
        }
    }
    AwtWindow(
        create = { frame },
        dispose = JFrame::dispose,
        update = { it.title = title },
    )
    LaunchedEffect(frame) {
        snapshotFlow { state.placement }.collect(frame::applyPlacement)
    }
    DisposableEffect(frame) {
        // Run once the content has had the key: Compose consumes the keys it uses
        val keys = KeyEventPostProcessor { event ->
            val inFrame = SwingUtilities.getWindowAncestor(event.component) === frame || event.component === frame
            if (event.isConsumed || event.id != AwtKeyEvent.KEY_PRESSED || !inFrame) return@KeyEventPostProcessor false
            shortcuts.onKeyDown(
                key = Key(event.keyCode, event.keyLocation),
                ctrl = event.isControlDown,
                alt = event.isAltDown,
                shift = event.isShiftDown,
                meta = event.isMetaDown,
            ).also { used -> if (used) event.consume() }
        }
        val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        focusManager.addKeyEventPostProcessor(keys)
        onDispose { focusManager.removeKeyEventPostProcessor(keys) }
    }
}

// The size Compose's window draws its icon at, in pixels
private val ICON_SIZE = Size(192f, 192f)

// Window state's sizes and positions are in AWT's units, one dp each, as Compose's window places itself
private fun JFrame.placeAt(state: WindowState) {
    if (state.size.isSpecified) {
        setSize(state.size.width.value.roundToInt(), state.size.height.value.roundToInt())
    }
    val position = state.position
    if (position is WindowPosition.Absolute) {
        setLocation(position.x.value.roundToInt(), position.y.value.roundToInt())
    } else {
        isLocationByPlatform = true
    }
    if (state.placement == WindowPlacement.Maximized) extendedState = Frame.MAXIMIZED_BOTH
}

private fun JFrame.followInto(state: WindowState) {
    addWindowStateListener { event ->
        state.isMinimized = event.newState and Frame.ICONIFIED != 0
        if (state.placement != WindowPlacement.Fullscreen) {
            state.placement = if (event.newState and Frame.MAXIMIZED_BOTH == Frame.MAXIMIZED_BOTH) {
                WindowPlacement.Maximized
            } else {
                WindowPlacement.Floating
            }
        }
    }
    addComponentListener(
        object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) {
                if (isFloating()) state.size = DpSize(width.dp, height.dp)
            }

            override fun componentMoved(event: ComponentEvent) {
                if (isFloating()) state.position = WindowPosition(x.dp, y.dp)
            }
        },
    )
}

private fun JFrame.isFloating(): Boolean =
    extendedState == Frame.NORMAL && graphicsConfiguration.device.fullScreenWindow !== this

private fun JFrame.applyPlacement(placement: WindowPlacement) {
    val device = graphicsConfiguration.device
    val fullScreen = placement == WindowPlacement.Fullscreen
    if (fullScreen != (device.fullScreenWindow === this)) device.fullScreenWindow = if (fullScreen) this else null
    if (!fullScreen) {
        val maximized = placement == WindowPlacement.Maximized
        extendedState = if (maximized) Frame.MAXIMIZED_BOTH else extendedState and Frame.MAXIMIZED_BOTH.inv()
    }
}
