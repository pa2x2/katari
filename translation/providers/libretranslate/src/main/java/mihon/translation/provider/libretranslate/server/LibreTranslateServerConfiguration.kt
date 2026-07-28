package mihon.translation.provider.libretranslate.server

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface LibreTranslateServerSettings {
    val endpoint: HttpUrl?
    val apiKey: String?
    val isInitiallyVerified: Boolean
    var disclosureAccepted: Boolean
}

internal class LibreTranslateServerConfiguration(
    private val state: LibreTranslateServerStateStore,
    private val secrets: LibreTranslateApiKeyStore,
) : LibreTranslateServerSettings {
    constructor(context: Context) : this(
        state = context.serverStateStore(),
        secrets = AndroidKeystoreApiKeyStore(context.serverPreferences()),
    )

    override val endpoint: HttpUrl?
        get() = validateEndpoint(state.endpoint.orEmpty())

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
        require(validateEndpoint(endpoint.toString()) == endpoint) {
            "LibreTranslate server endpoint must satisfy the provider security policy"
        }
        secrets.write(apiKey.orEmpty())
        state.saveEndpoint(
            endpoint = endpoint.toString(),
            verifiedEndpoint = endpoint.toString().takeIf { verified },
        )
    }

    companion object {
        fun validateEndpoint(value: String): HttpUrl? {
            val endpoint = value.trim().toHttpUrlOrNull() ?: return null
            if (endpoint.username.isNotEmpty() || endpoint.password.isNotEmpty()) return null
            if (endpoint.query != null || endpoint.fragment != null) return null
            val loopback = endpoint.host == "localhost" ||
                endpoint.host == "127.0.0.1" ||
                endpoint.host == "::1"
            if (endpoint.scheme != "https" && !(endpoint.scheme == "http" && loopback)) return null
            return endpoint.newBuilder()
                .encodedPath(endpoint.encodedPath.trimEnd('/') + "/")
                .build()
        }
    }
}

internal interface LibreTranslateServerStateStore {
    val endpoint: String?
    val verifiedEndpoint: String?
    var disclosureAccepted: Boolean

    fun saveEndpoint(
        endpoint: String,
        verifiedEndpoint: String?,
    )
}

internal interface LibreTranslateApiKeyStore {
    fun read(): String?
    fun write(value: String)
}

private class SharedPreferencesServerState(
    private val preferences: SharedPreferences,
) : LibreTranslateServerStateStore {
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

private class AndroidKeystoreApiKeyStore(
    private val preferences: android.content.SharedPreferences,
) : LibreTranslateApiKeyStore {
    override fun read(): String? {
        val encoded = preferences.getString(CIPHERTEXT_KEY, null) ?: return null
        val iv = preferences.getString(IV_KEY, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
            )
            cipher.doFinal(Base64.decode(encoded, Base64.NO_WRAP)).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    override fun write(value: String) {
        if (value.isBlank()) {
            preferences.edit().remove(CIPHERTEXT_KEY).remove(IV_KEY).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        preferences.edit()
            .putString(
                CIPHERTEXT_KEY,
                Base64.encodeToString(cipher.doFinal(value.toByteArray()), Base64.NO_WRAP),
            )
            .putString(IV_KEY, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance("AES", "AndroidKeyStore").run {
            init(
                android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                        android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val KEY_ALIAS = "katari.translation.libretranslate.api-key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val CIPHERTEXT_KEY = "api-key-ciphertext"
        const val IV_KEY = "api-key-iv"
    }
}

private fun Context.serverPreferences(): SharedPreferences {
    return getSharedPreferences("translation.libretranslate-server", Context.MODE_PRIVATE)
}

private fun Context.serverStateStore(): LibreTranslateServerStateStore {
    return SharedPreferencesServerState(serverPreferences())
}
