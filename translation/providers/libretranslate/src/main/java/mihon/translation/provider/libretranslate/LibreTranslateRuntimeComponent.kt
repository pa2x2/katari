package mihon.translation.provider.libretranslate

import android.app.Application
import mihon.feature.runtime.ApplicationFeatureRuntimeComponent
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorApplication
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorConfiguration
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorEngine
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorNetwork
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorSetup
import mihon.translation.provider.libretranslate.protocol.LibreTranslateHttpClient
import mihon.translation.provider.libretranslate.server.LibreTranslateServerConfiguration
import mihon.translation.provider.libretranslate.server.LibreTranslateServerEngine
import mihon.translation.provider.libretranslate.server.LibreTranslateServerNetwork
import mihon.translation.provider.libretranslate.server.LibreTranslateServerSetup
import mihon.translation.runtime.TranslationRuntimeComponent
import mihon.translation.runtime.TranslationRuntimeContribution
import mihon.translation.spi.TranslationEngineContribution

val libreTranslateRuntimeComponent: ApplicationFeatureRuntimeComponent =
    object : TranslationRuntimeComponent {
        override fun contribute(application: Application): TranslationRuntimeContribution {
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
            val serverConfiguration = LibreTranslateServerConfiguration(application)
            val serverEngine = LibreTranslateServerEngine(
                settings = serverConfiguration,
                serviceFactory = {
                    serverConfiguration.endpoint?.let { endpoint ->
                        LibreTranslateHttpClient(
                            httpClient = LibreTranslateServerNetwork.httpClient,
                            endpoint = endpoint,
                            apiKey = serverConfiguration.apiKey,
                        )
                    }
                },
            )
            return TranslationRuntimeContribution(
                engineContributions = listOf(
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
                        setup = LibreTranslateServerSetup(application, serverConfiguration),
                        order = LIBRETRANSLATE_SERVER_ORDER,
                    ),
                ),
            )
        }
    }

private const val OFFLINE_TRANSLATOR_ORDER = 100
private const val LIBRETRANSLATE_SERVER_ORDER = 200
