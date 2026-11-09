package app.katari.translation.provider.server.engine

import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.host.TranslationSetupDestination
import app.katari.translation.api.model.TranslationModelId
import app.katari.translation.api.model.TranslationModelOperationResult
import app.katari.translation.api.provider.TranslationProviderDisclosure
import app.katari.translation.provider.server.ServerConnectionSettings
import app.katari.translation.provider.server.setup.ProviderSetupScreen
import app.katari.translation.provider.server.setup.ServerSetupDestination
import app.katari.translation.spi.setup.TranslationEngineSetup
import app.katari.translation.spi.setup.TranslationSetupResult

/**
 * Sets up an engine that translates through a server of the user's own: the connection is configured on a screen
 * from [setupScreen], which [destination] opens, and accepting the [disclosure] is kept with the connection.
 */
class ServerEngineSetup(
    private val settings: ServerConnectionSettings,
    override val engine: TranslationEngineId,
    private val disclosure: TranslationProviderDisclosure,
    private val setupScreen: () -> ProviderSetupScreen,
    private val destination: ServerSetupDestination,
) : TranslationEngineSetup {
    override val supportsSetup = true

    override suspend fun acknowledge(disclosure: TranslationProviderDisclosure) {
        require(disclosure == this.disclosure)
        settings.disclosureAccepted = true
    }

    override suspend fun openSetup(): TranslationSetupResult = if (destination.open(setupScreen())) {
        TranslationSetupResult.Opened(TranslationSetupDestination.InApp)
    } else {
        TranslationSetupResult.SettingsUnavailable
    }

    override suspend fun downloadModels(
        models: Set<TranslationModelId>,
        allowMeteredNetwork: Boolean,
    ) = TranslationModelOperationResult.Failed("Language models are managed by the server")
}
