package app.katari.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.katari.desktop.di.DesktopGraph
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter

/**
 * Brings the hidden window back once nothing else could: when the tray stops showing the app's icon, as when the
 * desktop's tray ends, which would leave the app with no way back or out; and when something waits on an answer that
 * only shows in the window: an extension's dialog, a sign-in prompt or the question before quitting.
 */
@Composable
internal fun ReturnsFromTray(graph: DesktopGraph) {
    LaunchedEffect(graph) {
        val asking = combine(
            graph.windowExtensionDialogs.dialogs,
            graph.signInPrompts.current,
            graph.workCloseGuard.isAsking,
        ) { dialogs, signIn, quitting -> dialogs.isNotEmpty() || signIn != null || quitting }
        combine(graph.mainWindowVisibility.isHidden, graph.appTray.isShown, asking) { hidden, inTray, waiting ->
            hidden && (!inTray || waiting)
        }
            .filter { it }
            .collect { graph.mainWindowVisibility.bringForward(activationToken = null) }
    }
}
