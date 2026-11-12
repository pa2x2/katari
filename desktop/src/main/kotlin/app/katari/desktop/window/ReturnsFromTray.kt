package app.katari.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.katari.desktop.di.DesktopGraph
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Brings the hidden window back once nothing else could: when something waits on an answer that only shows in the
 * window, an extension's dialog, a sign-in prompt or the question before quitting; and when no tray shows the app's
 * icon, as when the desktop's tray ends, which would leave the app with no way back or out. After a start in the tray,
 * as at login, the desktop's tray may start after the app, so the window waits for it for [TRAY_AT_START_WAIT] first.
 */
@Composable
internal fun ReturnsFromTray(graph: DesktopGraph) {
    val visibility = graph.mainWindowVisibility
    LaunchedEffect(graph) {
        launch {
            combine(
                graph.windowExtensionDialogs.dialogs,
                graph.signInPrompts.current,
                graph.workCloseGuard.isAsking,
                visibility.isHidden,
            ) { dialogs, signIn, quitting, hidden -> hidden && (dialogs.isNotEmpty() || signIn != null || quitting) }
                .filter { it }
                .collect { visibility.bringForward(activationToken = null) }
        }
        if (visibility.startedInTray) withTimeoutOrNull(TRAY_AT_START_WAIT) { graph.appTray.isShown.first { it } }
        combine(visibility.isHidden, graph.appTray.isShown) { hidden, inTray -> hidden && !inTray }
            .filter { it }
            .collect { visibility.bringForward(activationToken = null) }
    }
}

private val TRAY_AT_START_WAIT = 60.seconds
