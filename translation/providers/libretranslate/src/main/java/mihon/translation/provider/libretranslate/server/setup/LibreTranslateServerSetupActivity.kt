package mihon.translation.provider.libretranslate.server.setup

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.tachiyomi.network.localnetwork.guardingLocalNetworkAccess
import mihon.translation.provider.libretranslate.R
import mihon.translation.provider.libretranslate.protocol.LibreTranslateHttpClient
import mihon.translation.provider.libretranslate.server.LibreTranslateServerNetwork
import mihon.translation.provider.libretranslate.server.libreTranslateServerConfiguration
import mihon.translation.provider.server.setup.ServerSetupCoordinator
import mihon.translation.provider.server.setup.ServerSetupRoute
import mihon.translation.provider.server.setup.ServerSetupTexts

internal class LibreTranslateServerSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val configuration = libreTranslateServerConfiguration(applicationContext)
        val httpClient = LibreTranslateServerNetwork.httpClient.guardingLocalNetworkAccess(applicationContext)
        val coordinator = ServerSetupCoordinator(
            verify = { endpoint, apiKey ->
                LibreTranslateHttpClient(
                    httpClient = httpClient,
                    endpoint = endpoint,
                    apiKey = apiKey,
                ).languages().isNotEmpty()
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
                        title = stringResource(R.string.libretranslate_server_setup_title),
                        description = stringResource(R.string.libretranslate_server_setup_description),
                        apiKeyLabel = stringResource(R.string.libretranslate_server_api_key),
                        connectionFailed = stringResource(R.string.libretranslate_server_connection_failed),
                    ),
                    artworkResourceId = R.drawable.ic_libretranslate_server,
                    onBack = ::finish,
                    onDone = ::finish,
                )
            }
        }
    }
}
