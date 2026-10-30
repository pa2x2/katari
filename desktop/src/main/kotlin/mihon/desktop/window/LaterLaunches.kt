package mihon.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow
import mihon.desktop.application.instance.InstanceRequest
import mihon.desktop.display.backend.DisplayBackend
import java.awt.Frame

/** Brings [window] forward through [displayBackend] for each of [laterLaunches], on the window's thread. */
@Composable
internal fun BringForwardForLaterLaunches(
    window: Frame,
    laterLaunches: Flow<InstanceRequest>,
    displayBackend: DisplayBackend,
) {
    LaunchedEffect(window, laterLaunches) {
        laterLaunches.collect { request -> displayBackend.bringForward(window, request.activationToken) }
    }
}
