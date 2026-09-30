package mihon.translation.provider.deepl

import android.app.Application
import eu.kanade.tachiyomi.network.localnetwork.guardingLocalNetworkAccess
import mihon.feature.runtime.application.ApplicationFeatureRuntimeComponent
import mihon.translation.provider.deepl.protocol.DeepLHttpClient
import mihon.translation.provider.deepl.setup.DeepLServerSetupActivity
import mihon.translation.provider.server.engine.ServerEngineSetup
import mihon.translation.runtime.component.TranslationRuntimeComponent
import mihon.translation.runtime.component.TranslationRuntimeContribution
import mihon.translation.spi.contribution.TranslationEngineContribution

val deepLRuntimeComponent: ApplicationFeatureRuntimeComponent =
    object : TranslationRuntimeComponent {
        override fun contribute(application: Application): TranslationRuntimeContribution {
            val configuration = deepLServerConfiguration(application)
            val httpClient = DeepLServerNetwork.httpClient.guardingLocalNetworkAccess(application)
            val engine = DeepLServerEngine(
                settings = configuration,
                serviceFactory = {
                    configuration.endpoint?.let { endpoint ->
                        DeepLHttpClient(
                            httpClient = httpClient,
                            endpoint = endpoint,
                            apiKey = configuration.apiKey,
                        )
                    }
                },
            )
            return TranslationRuntimeContribution(
                engineContributions = listOf(
                    TranslationEngineContribution(
                        engine = engine,
                        setup = ServerEngineSetup(
                            context = application,
                            settings = configuration,
                            engine = DeepLServerEngine.ENGINE_ID,
                            disclosure = DeepLServerEngine.DISCLOSURE,
                            setupActivity = DeepLServerSetupActivity::class.java,
                        ),
                        order = DEEPL_SERVER_ORDER,
                    ),
                ),
            )
        }
    }

private const val DEEPL_SERVER_ORDER = 300
