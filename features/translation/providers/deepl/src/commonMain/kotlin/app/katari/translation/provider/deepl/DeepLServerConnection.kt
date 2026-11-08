package app.katari.translation.provider.deepl

import app.katari.translation.provider.deepl.protocol.DeepLHttpClient
import app.katari.translation.provider.server.SecretStore
import app.katari.translation.provider.server.ServerConnectionConfiguration
import app.katari.translation.provider.server.ServerConnectionId
import app.katari.translation.provider.server.ServerConnectionPreferenceStores
import app.katari.translation.provider.server.ServerLocalNetworkGuard
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import okhttp3.HttpUrl

/** The DeepL-compatible server connection the engine translates through and its setup screen configures. */
@SingleIn(AppScope::class)
@Inject
internal class DeepLServerConnection(
    preferenceStores: ServerConnectionPreferenceStores,
    secrets: SecretStore,
    localNetworkGuard: ServerLocalNetworkGuard,
) {
    val configuration = ServerConnectionConfiguration(ID, preferenceStores, secrets)
    private val httpClient = localNetworkGuard.guard(DeepLServerNetwork.httpClient)

    fun client(endpoint: HttpUrl, apiKey: String?) = DeepLHttpClient(
        httpClient = httpClient,
        endpoint = endpoint,
        apiKey = apiKey,
    )

    private companion object {
        val ID = ServerConnectionId(
            preferencesName = "translation.deepl-server",
            apiKeyAlias = "katari.translation.deepl.api-key",
            apiKeyLabel = "Katari: DeepL-compatible server API key",
        )
    }
}
