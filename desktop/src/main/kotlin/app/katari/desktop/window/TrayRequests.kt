package app.katari.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.katari.desktop.tray.AppTray
import app.katari.desktop.tray.TrayCommand
import app.katari.desktop.work.WorkCloseGuard

/**
 * Answers what [tray] asks for on the window's thread: opening brings the window forward through [visibility], and
 * quitting goes through [onQuit], bringing the window forward first when [closeGuard] asks before the app quits, as
 * its question shows in the window.
 */
@Composable
internal fun AnswerTrayRequests(
    tray: AppTray,
    visibility: MainWindowVisibility,
    closeGuard: WorkCloseGuard,
    onQuit: () -> Unit,
) {
    LaunchedEffect(tray) {
        tray.requests.collect { request ->
            when (request.command) {
                TrayCommand.Open -> visibility.bringForward(request.activationToken)
                TrayCommand.Quit -> {
                    onQuit()
                    if (closeGuard.isAsking.value) visibility.bringForward(request.activationToken)
                }
            }
        }
    }
}
