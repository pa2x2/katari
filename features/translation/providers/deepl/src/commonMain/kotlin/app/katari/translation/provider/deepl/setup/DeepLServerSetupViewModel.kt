package app.katari.translation.provider.deepl.setup

import androidx.lifecycle.ViewModel
import app.katari.translation.provider.deepl.DeepLServerConnection
import app.katari.translation.provider.server.SecretStore
import app.katari.translation.provider.server.setup.ServerSetupCoordinator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey

/** The connection [DeepLServerSetupScreen] shows when it opens, and what tests and saves the one the user enters. */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class DeepLServerSetupViewModel internal constructor(
    connection: DeepLServerConnection,
    secrets: SecretStore,
) : ViewModel() {
    private val configuration = connection.configuration

    val initialEndpoint: String = configuration.endpoint?.toString().orEmpty()
    val initialApiKey: String = configuration.apiKey.orEmpty()
    val keyStorage = secrets.description

    val coordinator = ServerSetupCoordinator(
        verify = { endpoint, apiKey ->
            val languages = connection.client(endpoint, apiKey).languages()
            languages.sources.isNotEmpty() && languages.targets.isNotEmpty()
        },
        saveConfiguration = configuration::save,
    )
}
