package mihon.translation.provider.libretranslate

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.Provides
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorApplication
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorConfiguration
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorEngine
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorNetwork
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorSetup
import mihon.translation.provider.libretranslate.protocol.LibreTranslateHttpClient
import mihon.translation.provider.libretranslate.server.LibreTranslateServerEngine
import mihon.translation.provider.libretranslate.server.LibreTranslateServerNetwork
import mihon.translation.provider.libretranslate.server.libreTranslateServerConfiguration
import mihon.translation.provider.libretranslate.server.setup.LibreTranslateServerSetupActivity
import mihon.translation.provider.server.engine.ServerEngineSetup
import mihon.translation.provider.server.guardingServerLocalNetworkAccess
import mihon.translation.spi.contribution.TranslationEngineContribution

@ContributesTo(AppScope::class)
@BindingContainer
object LibreTranslateBindings {
    @Provides
    @ElementsIntoSet
    fun engines(application: Application): Set<TranslationEngineContribution> {
        val settings = OfflineTranslatorConfiguration(application)
        val providerApplication = OfflineTranslatorApplication(application)
        val engine = OfflineTranslatorEngine(
            application = providerApplication,
            settings = settings,
            serviceFactory = { endpoint ->
                LibreTranslateHttpClient(
                    httpClient = OfflineTranslatorNetwork.httpClient,
                    endpoint = endpoint,
                )
            },
        )
        val serverConfiguration = libreTranslateServerConfiguration(application)
        val serverHttpClient = LibreTranslateServerNetwork.httpClient.guardingServerLocalNetworkAccess(application)
        val serverEngine = LibreTranslateServerEngine(
            settings = serverConfiguration,
            serviceFactory = {
                serverConfiguration.endpoint?.let { endpoint ->
                    LibreTranslateHttpClient(
                        httpClient = serverHttpClient,
                        endpoint = endpoint,
                        apiKey = serverConfiguration.apiKey,
                    )
                }
            },
        )
        return setOf(
            TranslationEngineContribution(
                engine = engine,
                setup = OfflineTranslatorSetup(
                    context = application,
                    application = providerApplication,
                    settings = settings,
                ),
                order = OFFLINE_TRANSLATOR_ORDER,
            ),
            TranslationEngineContribution(
                engine = serverEngine,
                setup = ServerEngineSetup(
                    context = application,
                    settings = serverConfiguration,
                    engine = LibreTranslateServerEngine.ENGINE_ID,
                    disclosure = LibreTranslateServerEngine.DISCLOSURE,
                    setupActivity = LibreTranslateServerSetupActivity::class.java,
                ),
                order = LIBRETRANSLATE_SERVER_ORDER,
            ),
        )
    }
}

private const val OFFLINE_TRANSLATOR_ORDER = 100
private const val LIBRETRANSLATE_SERVER_ORDER = 200
