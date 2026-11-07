package mihon.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import mihon.app.ui.host.AppProcessGraph
import mihon.app.ui.host.AppVisibility
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.JFrame

/**
 * Tells [AppVisibility] the app came to the front when [window] gains focus, and went to the back when it loses focus
 * or closes: on a desktop, where the window stays open behind others, focus is what Android's foreground is.
 */
@Composable
internal fun ReportsForeground(window: JFrame, graph: AppProcessGraph) {
    DisposableEffect(window) {
        val listener = object : WindowFocusListener {
            override fun windowGainedFocus(event: WindowEvent) = AppVisibility.cameToFront(graph)

            override fun windowLostFocus(event: WindowEvent) = AppVisibility.wentToBack(graph)
        }
        window.addWindowFocusListener(listener)
        if (window.isFocused) AppVisibility.cameToFront(graph)
        onDispose {
            window.removeWindowFocusListener(listener)
            AppVisibility.wentToBack(graph)
        }
    }
}
