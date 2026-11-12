package app.katari.translation.provider.libretranslate.server.setup

import androidx.lifecycle.ViewModel
import app.katari.translation.provider.libretranslate.server.LibreTranslateServerConnection
import app.katari.translation.provider.server.SecretStore
import app.katari.translation.provider.server.setup.ServerSetupCoordinator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey

/**
 * The connection [LibreTranslateServerSetupScreen] shows when it opens, and what tests and saves the one the user
 * enters.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class LibreTranslateServerSetupViewModel internal constructor(
    connection: LibreTranslateServerConnection,
    secrets: SecretStore,
) : ViewModel() {
    private val configuration = connection.configuration

    val initialEndpoint: String = configuration.endpoint?.toString().orEmpty()
    val initialApiKey: String = configuration.askForApiKey().orEmpty()
    val keyStorage = secrets.description

    val coordinator = ServerSetupCoordinator(
        verify = { endpoint, apiKey -> connection.client(endpoint, apiKey).languages().isNotEmpty() },
        saveConfiguration = configuration::save,
    )
}
