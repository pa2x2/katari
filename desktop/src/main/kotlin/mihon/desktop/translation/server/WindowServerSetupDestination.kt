package mihon.desktop.translation.server

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.desktop.window.OpenWindow
import mihon.translation.provider.server.setup.ProviderSetupScreen
import mihon.translation.provider.server.setup.ServerSetupDestination

/** Opens a provider's setup screen on the navigator of the app's window, over whatever it shows. */
@Inject
class WindowServerSetupDestination(private val window: OpenWindow) : ServerSetupDestination {
    override suspend fun open(screen: ProviderSetupScreen): Boolean = withContext(Dispatchers.Main) {
        val navigator = window.context?.navigator ?: return@withContext false
        navigator.push(screen)
        true
    }
}
