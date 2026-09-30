package mihon.translation.provider.server

import android.content.Context
import android.content.SharedPreferences
import okhttp3.HttpUrl

/** A translation server the user configured: where it is, its key, and what the user accepted about it. */
interface ServerConnectionSettings {
    val endpoint: HttpUrl?
    val apiKey: String?

    /** Whether the saved endpoint passed a connection test when it was saved. */
    val isInitiallyVerified: Boolean
    var disclosureAccepted: Boolean
}

/**
 * Stores one provider's server connection in the [preferencesName] preferences, with the API key encrypted under the
 * Android Keystore key [apiKeyAlias].
 */
class ServerConnectionConfiguration internal constructor(
    private val state: ServerConnectionStateStore,
    private val secrets: ServerApiKeyStore,
) : ServerConnectionSettings {
    constructor(context: Context, preferencesName: String, apiKeyAlias: String) : this(
        state = SharedPreferencesConnectionState(context.preferences(preferencesName)),
        secrets = AndroidKeystoreApiKeyStore(context.preferences(preferencesName), apiKeyAlias),
    )

    override val endpoint: HttpUrl?
        get() = ServerEndpointPolicy.validate(state.endpoint.orEmpty())

    override val apiKey: String?
        get() = secrets.read()?.takeIf(String::isNotBlank)

    override val isInitiallyVerified: Boolean
        get() = endpoint?.toString() == state.verifiedEndpoint

    override var disclosureAccepted: Boolean
        get() = state.disclosureAccepted
        set(value) {
            state.disclosureAccepted = value
        }

    fun save(
        endpoint: HttpUrl,
        apiKey: String?,
        verified: Boolean,
    ) {
        require(ServerEndpointPolicy.validate(endpoint.toString()) == endpoint) {
            "Server endpoint must satisfy the provider security policy"
        }
        secrets.write(apiKey.orEmpty())
        state.saveEndpoint(
            endpoint = endpoint.toString(),
            verifiedEndpoint = endpoint.toString().takeIf { verified },
        )
    }
}

internal interface ServerConnectionStateStore {
    val endpoint: String?
    val verifiedEndpoint: String?
    var disclosureAccepted: Boolean

    fun saveEndpoint(
        endpoint: String,
        verifiedEndpoint: String?,
    )
}

private class SharedPreferencesConnectionState(
    private val preferences: SharedPreferences,
) : ServerConnectionStateStore {
    override val endpoint: String?
        get() = preferences.getString(ENDPOINT_KEY, null)

    override val verifiedEndpoint: String?
        get() = preferences.getString(VERIFIED_ENDPOINT_KEY, null)

    override var disclosureAccepted: Boolean
        get() = preferences.getBoolean(DISCLOSURE_KEY, false)
        set(value) {
            preferences.edit().putBoolean(DISCLOSURE_KEY, value).apply()
        }

    override fun saveEndpoint(
        endpoint: String,
        verifiedEndpoint: String?,
    ) {
        preferences.edit()
            .putString(ENDPOINT_KEY, endpoint)
            .putString(VERIFIED_ENDPOINT_KEY, verifiedEndpoint)
            .apply()
    }

    private companion object {
        const val ENDPOINT_KEY = "endpoint"
        const val VERIFIED_ENDPOINT_KEY = "verified-endpoint"
        const val DISCLOSURE_KEY = "provider-disclosure-accepted"
    }
}

private fun Context.preferences(name: String): SharedPreferences = getSharedPreferences(name, Context.MODE_PRIVATE)
