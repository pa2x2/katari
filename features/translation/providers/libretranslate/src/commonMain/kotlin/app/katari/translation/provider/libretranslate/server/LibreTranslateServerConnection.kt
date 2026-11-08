package app.katari.translation.provider.libretranslate.server

import app.katari.translation.provider.libretranslate.protocol.LibreTranslateHttpClient
import app.katari.translation.provider.server.SecretStore
import app.katari.translation.provider.server.ServerConnectionConfiguration
import app.katari.translation.provider.server.ServerConnectionId
import app.katari.translation.provider.server.ServerConnectionPreferenceStores
import app.katari.translation.provider.server.ServerLocalNetworkGuard
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import okhttp3.HttpUrl

/** The LibreTranslate server connection the engine translates through and its setup screen configures. */
@SingleIn(AppScope::class)
@Inject
internal class LibreTranslateServerConnection(
    preferenceStores: ServerConnectionPreferenceStores,
    secrets: SecretStore,
    localNetworkGuard: ServerLocalNetworkGuard,
) {
    val configuration = ServerConnectionConfiguration(ID, preferenceStores, secrets)
    private val httpClient = localNetworkGuard.guard(LibreTranslateServerNetwork.httpClient)

    fun client(endpoint: HttpUrl, apiKey: String?) = LibreTranslateHttpClient(
        httpClient = httpClient,
        endpoint = endpoint,
        apiKey = apiKey,
    )

    private companion object {
        val ID = ServerConnectionId(
            preferencesName = "translation.libretranslate-server",
            apiKeyAlias = "katari.translation.libretranslate.api-key",
            apiKeyLabel = "Katari: LibreTranslate server API key",
        )
    }
}
