package mihon.translation.provider.deepl

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.network.localnetwork.guardingLocalNetworkAccess
import mihon.translation.provider.deepl.protocol.DeepLHttpClient
import mihon.translation.provider.deepl.setup.DeepLServerSetupActivity
import mihon.translation.provider.server.engine.ServerEngineSetup
import mihon.translation.spi.contribution.TranslationEngineContribution

@ContributesTo(AppScope::class)
@BindingContainer
object DeepLBindings {
    @Provides
    @IntoSet
    fun engine(application: Application): TranslationEngineContribution {
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
        return TranslationEngineContribution(
            engine = engine,
            setup = ServerEngineSetup(
                context = application,
                settings = configuration,
                engine = DeepLServerEngine.ENGINE_ID,
                disclosure = DeepLServerEngine.DISCLOSURE,
                setupActivity = DeepLServerSetupActivity::class.java,
            ),
            order = DEEPL_SERVER_ORDER,
        )
    }
}

private const val DEEPL_SERVER_ORDER = 300
