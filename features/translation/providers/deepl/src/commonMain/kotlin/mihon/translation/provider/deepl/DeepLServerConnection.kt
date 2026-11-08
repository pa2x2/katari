package mihon.translation.provider.deepl

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import mihon.translation.provider.deepl.protocol.DeepLHttpClient
import mihon.translation.provider.server.SecretStore
import mihon.translation.provider.server.ServerConnectionConfiguration
import mihon.translation.provider.server.ServerConnectionId
import mihon.translation.provider.server.ServerConnectionPreferenceStores
import mihon.translation.provider.server.ServerLocalNetworkGuard
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
