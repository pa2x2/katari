package app.katari.translation.provider.server

import logcat.LogPriority
import okhttp3.HttpUrl
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat
import java.io.IOException

/** A translation server the user configured: where it is, its key, and what the user accepted about it. */
interface ServerConnectionSettings {
    val endpoint: HttpUrl?

    /** Null when none is saved, or while the store can't be read, when a call goes without it and the server says so. */
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

            override fun allowAsking() = secrets.allowAsking(connection)

            override fun write(value: String) = secrets.write(connection, value)
        },
    )

    override val endpoint: HttpUrl?
        get() = ServerEndpointPolicy.validate(state.endpoint.orEmpty())

    override val apiKey: String?
        get() = try {
            secrets.read()?.takeIf(String::isNotBlank)
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Can't read a translation server's API key now" }
            null
        }

    /** The key, for a screen the person opened, which may ask them to unlock the store ([SecretStore.allowAsking]). */
    fun askForApiKey(): String? {
        secrets.allowAsking()
        return apiKey
    }

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
    /** As [SecretStore.read] for one connection: null when none is saved, throwing when the store can't be read now. */
    fun read(): String?
    fun allowAsking()
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
