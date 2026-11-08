package app.katari.translation.provider.server

import okhttp3.HttpUrl
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** A translation server the user configured: where it is, its key, and what the user accepted about it. */
interface ServerConnectionSettings {
    val endpoint: HttpUrl?
    val apiKey: String?

    /** Whether the saved endpoint passed a connection test when it was saved. */
    val isInitiallyVerified: Boolean
    var disclosureAccepted: Boolean
}

/** Stores one provider's server connection, [connection], with its API key in [secrets] and the rest in [state]. */
class ServerConnectionConfiguration internal constructor(
    private val state: ServerConnectionStateStore,
    private val secrets: ServerApiKeyStore,
) : ServerConnectionSettings {
    constructor(
        connection: ServerConnectionId,
        preferenceStores: ServerConnectionPreferenceStores,
        secrets: SecretStore,
    ) : this(
        state = PreferenceConnectionState(preferenceStores.store(connection)),
        secrets = object : ServerApiKeyStore {
            override fun read(): String? = secrets.read(connection)

            override fun write(value: String) = secrets.write(connection, value)
        },
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

internal interface ServerApiKeyStore {
    fun read(): String?
    fun write(value: String)
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

private class PreferenceConnectionState(store: PreferenceStore) : ServerConnectionStateStore {
    private val savedEndpoint = store.getString(ENDPOINT_KEY, "")
    private val savedVerifiedEndpoint = store.getString(VERIFIED_ENDPOINT_KEY, "")
    private val savedDisclosureAccepted = store.getBoolean(DISCLOSURE_KEY, false)

    override val endpoint: String?
        get() = savedEndpoint.savedValue()

    override val verifiedEndpoint: String?
        get() = savedVerifiedEndpoint.savedValue()

    override var disclosureAccepted: Boolean
        get() = savedDisclosureAccepted.get()
        set(value) {
            savedDisclosureAccepted.set(value)
        }

    override fun saveEndpoint(
        endpoint: String,
        verifiedEndpoint: String?,
    ) {
        savedEndpoint.set(endpoint)
        if (verifiedEndpoint == null) savedVerifiedEndpoint.delete() else savedVerifiedEndpoint.set(verifiedEndpoint)
    }

    private fun Preference<String>.savedValue(): String? = get().takeIf { isSet() }

    private companion object {
        const val ENDPOINT_KEY = "endpoint"
        const val VERIFIED_ENDPOINT_KEY = "verified-endpoint"
        const val DISCLOSURE_KEY = "provider-disclosure-accepted"
    }
}
