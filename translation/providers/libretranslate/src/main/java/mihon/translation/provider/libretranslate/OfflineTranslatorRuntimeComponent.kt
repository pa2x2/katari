package mihon.translation.provider.libretranslate

import android.app.Application
import mihon.feature.runtime.ApplicationFeatureRuntimeComponent
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorApplication
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorConfiguration
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorEngine
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorNetwork
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorSetup
import mihon.translation.provider.libretranslate.protocol.LibreTranslateHttpClient
import mihon.translation.runtime.TranslationRuntimeComponent
import mihon.translation.runtime.TranslationRuntimeContribution
import mihon.translation.spi.TranslationEngineContribution

val offlineTranslatorRuntimeComponent: ApplicationFeatureRuntimeComponent =
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
                ),
            )
        }
    }

private const val OFFLINE_TRANSLATOR_ORDER = 100
