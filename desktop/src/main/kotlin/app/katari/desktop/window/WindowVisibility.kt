package app.katari.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import app.katari.desktop.presence.AppPresence
import app.katari.ui.host.AppProcessGraph
import app.katari.ui.host.AppVisibility
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.JFrame

/**
 * Tells [AppVisibility] the app shows while the main window is open and not minimized, as [presence] has it from
 * [app.katari.desktop.presence.ReportsPresence], and is hidden once it is minimized or closes. A window covered by
 * others still shows, since X11 doesn't tell whether it is covered.
 */
@Composable
internal fun ReportsShown(graph: AppProcessGraph, presence: AppPresence) {
    LaunchedEffect(presence) {
        try {
            presence.isMainWindowMinimized.collect { minimized ->
                if (minimized) AppVisibility.becameHidden(graph) else AppVisibility.becameShown(graph)
            }
        } finally {
            AppVisibility.becameHidden(graph)
        }
    }
}

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
