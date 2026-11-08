package app.katari.translation.provider.libretranslate.server

import app.katari.translation.provider.libretranslate.server.setup.LibreTranslateServerSetupScreen
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
object LibreTranslateServerBindings {
    // Without a store for its API key, the engine is not offered.
    @Provides
    @ElementsIntoSet
    internal fun engine(
        connection: LibreTranslateServerConnection,
        secrets: SecretStore,
        setupDestination: ServerSetupDestination,
    ): Set<TranslationEngineContribution> {
        if (!secrets.isAvailable) return emptySet()
        val configuration = connection.configuration
        val engine = LibreTranslateServerEngine(
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
                    engine = LibreTranslateServerEngine.ENGINE_ID,
                    disclosure = LibreTranslateServerEngine.DISCLOSURE,
                    setupScreen = LibreTranslateServerSetupScreen,
                    destination = setupDestination,
                ),
                order = LIBRETRANSLATE_SERVER_ORDER,
            ),
        )
    }
}

private const val LIBRETRANSLATE_SERVER_ORDER = 200
