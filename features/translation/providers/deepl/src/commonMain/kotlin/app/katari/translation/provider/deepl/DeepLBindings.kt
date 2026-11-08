package app.katari.translation.provider.deepl

import app.katari.translation.provider.deepl.setup.DeepLServerSetupScreen
import app.katari.translation.provider.server.SecretStore
import app.katari.translation.provider.server.engine.ServerEngineSetup
import app.katari.translation.provider.server.setup.ServerSetupDestination
import app.katari.translation.spi.contribution.TranslationEngineContribution
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
object DeepLBindings {
    // Without a store for its API key, the engine is not offered.
    @Provides
    @ElementsIntoSet
    internal fun engine(
        connection: DeepLServerConnection,
        secrets: SecretStore,
        setupDestination: ServerSetupDestination,
    ): Set<TranslationEngineContribution> {
        if (!secrets.isAvailable) return emptySet()
        val configuration = connection.configuration
        val engine = DeepLServerEngine(
            settings = configuration,
            serviceFactory = {
                configuration.endpoint?.let { endpoint -> connection.client(endpoint, configuration.apiKey) }
            },
        )
        return setOf(
            TranslationEngineContribution(
                engine = engine,
                setup = ServerEngineSetup(
                    settings = configuration,
                    engine = DeepLServerEngine.ENGINE_ID,
                    disclosure = DeepLServerEngine.DISCLOSURE,
                    setupScreen = DeepLServerSetupScreen,
                    destination = setupDestination,
                ),
                order = DEEPL_SERVER_ORDER,
            ),
        )
    }
}

private const val DEEPL_SERVER_ORDER = 300
