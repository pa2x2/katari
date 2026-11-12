package app.katari.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.katari.desktop.application.instance.InstanceRequest
import app.katari.desktop.application.launch.LaunchLinks
import app.katari.desktop.home.HomeDestinations
import kotlinx.coroutines.flow.Flow

/**
 * Answers each of [laterLaunches] on the window's thread: brings the window forward through [visibility], hidden in
 * the tray or not, unless the launch starts in the tray, then opens the launch's links in [destinations].
 */
@Composable
internal fun AnswerLaterLaunches(
    laterLaunches: Flow<InstanceRequest>,
    visibility: MainWindowVisibility,
    destinations: HomeDestinations,
) {
    LaunchedEffect(laterLaunches) {
        laterLaunches.collect { request ->
            if (!request.startsInTray) visibility.bringForward(request.activationToken)
            LaunchLinks.open(request.links, destinations)
        }
    }
}
