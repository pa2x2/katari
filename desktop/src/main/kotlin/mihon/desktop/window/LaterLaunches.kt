package mihon.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow
import mihon.desktop.application.instance.InstanceRequest
import mihon.desktop.application.launch.LaunchLinks
import mihon.desktop.display.backend.DisplayBackend
import mihon.desktop.home.HomeDestinations
import java.awt.Frame

/**
 * Answers each of [laterLaunches] on the window's thread: brings [window] forward through [displayBackend], then opens
 * the launch's links in [destinations].
 */
@Composable
internal fun AnswerLaterLaunches(
    window: Frame,
    laterLaunches: Flow<InstanceRequest>,
    displayBackend: DisplayBackend,
    destinations: HomeDestinations,
) {
    LaunchedEffect(window, laterLaunches) {
        laterLaunches.collect { request ->
            displayBackend.bringForward(window, request.activationToken)
            LaunchLinks.open(request.links, destinations)
        }
    }
}
