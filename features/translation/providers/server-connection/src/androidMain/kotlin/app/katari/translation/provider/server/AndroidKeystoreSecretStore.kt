package app.katari.translation.provider.server

import android.content.Context
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.server_connection_security_description
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps each connection's API key encrypted under the Android Keystore key named by its
 * [ServerConnectionId.apiKeyAlias], as ciphertext in the connection's own preferences file.
 */
internal class AndroidKeystoreSecretStore(private val context: Context) : SecretStore {
    private val stores = ConcurrentHashMap<String, AndroidKeystoreApiKeyStore>()

    override val isAvailable: Boolean = true

    override val description: StringResource = MR.strings.server_connection_security_description

    override fun read(connection: ServerConnectionId): String? = store(connection).read()

    override fun write(connection: ServerConnectionId, value: String) = store(connection).write(value)

    private fun store(connection: ServerConnectionId): AndroidKeystoreApiKeyStore =
        stores.getOrPut(connection.apiKeyAlias) {
            AndroidKeystoreApiKeyStore(
                preferences = context.getSharedPreferences(connection.preferencesName, Context.MODE_PRIVATE),
                keyAlias = connection.apiKeyAlias,
            )
        }
}
