package app.katari.sync.host

import app.katari.sync.client.connection.DeviceTokenStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import mihon.translation.provider.server.SecretStore
import mihon.translation.provider.server.ServerConnectionId

/**
 * The sync device token in the platform's store for secrets, where translation servers' API keys are kept too:
 * encrypted under an Android Keystore key, or in the desktop's keyring.
 */
@Inject
@ContributesBinding(AppScope::class)
class SecretStoreDeviceTokens(private val secrets: SecretStore) : DeviceTokenStore {
    override val isAvailable: Boolean get() = secrets.isAvailable

    override fun read(): String? = secrets.read(TOKEN)

    // The store forgets a key when given a blank one.
    override fun write(token: String?) = secrets.write(TOKEN, token.orEmpty())

    private companion object {
        val TOKEN = ServerConnectionId(
            preferencesName = "sync.device-token",
            apiKeyAlias = "katari_sync_device_token",
            apiKeyLabel = "Katari sync device token",
        )
    }
}
