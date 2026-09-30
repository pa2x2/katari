package mihon.translation.provider.deepl.setup

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.tachiyomi.network.localnetwork.guardingLocalNetworkAccess
import mihon.translation.provider.deepl.DeepLServerNetwork
import mihon.translation.provider.deepl.R
import mihon.translation.provider.deepl.deepLServerConfiguration
import mihon.translation.provider.deepl.protocol.DeepLHttpClient
import mihon.translation.provider.server.setup.ServerEndpointSuggestion
import mihon.translation.provider.server.setup.ServerSetupCoordinator
import mihon.translation.provider.server.setup.ServerSetupRoute
import mihon.translation.provider.server.setup.ServerSetupTexts

internal class DeepLServerSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val configuration = deepLServerConfiguration(applicationContext)
        val httpClient = DeepLServerNetwork.httpClient.guardingLocalNetworkAccess(applicationContext)
        val coordinator = ServerSetupCoordinator(
            verify = { endpoint, apiKey ->
                val languages = DeepLHttpClient(httpClient, endpoint, apiKey).languages()
                languages.sources.isNotEmpty() && languages.targets.isNotEmpty()
            },
            saveConfiguration = configuration::save,
        )
        setContent {
            TachiyomiTheme {
                ServerSetupRoute(
                    initialEndpoint = configuration.endpoint?.toString().orEmpty(),
                    initialApiKey = configuration.apiKey.orEmpty(),
                    coordinator = coordinator,
                    texts = ServerSetupTexts(
                        title = stringResource(R.string.deepl_server_setup_title),
                        description = stringResource(R.string.deepl_server_setup_description),
                        apiKeyLabel = stringResource(R.string.deepl_server_api_key),
                        connectionFailed = stringResource(R.string.deepl_server_connection_failed),
                    ),
                    artworkResourceId = R.drawable.ic_deepl_compatible_server,
                    onBack = ::finish,
                    onDone = ::finish,
                    endpointSuggestions = listOf(
                        ServerEndpointSuggestion(
                            label = stringResource(R.string.deepl_server_suggestion_free),
                            endpoint = "https://api-free.deepl.com/",
                        ),
                        ServerEndpointSuggestion(
                            label = stringResource(R.string.deepl_server_suggestion_pro),
                            endpoint = "https://api.deepl.com/",
                        ),
                    ),
                )
            }
        }
    }
}
