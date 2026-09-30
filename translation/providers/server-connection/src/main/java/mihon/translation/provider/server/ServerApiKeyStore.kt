package mihon.translation.provider.server

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface ServerApiKeyStore {
    fun read(): String?
    fun write(value: String)
}

/**
 * Keeps an API key in [preferences] only as ciphertext of the Android Keystore key [keyAlias].
 *
 * The key is read before every call to the server, so it is decrypted once and remembered for as long as the stored
 * ciphertext stays the one it was decrypted from, whichever store wrote it.
 */
internal class AndroidKeystoreApiKeyStore(
    private val preferences: SharedPreferences,
    private val keyAlias: String,
) : ServerApiKeyStore {
    @Volatile
    private var decrypted: Decrypted? = null

    override fun read(): String? {
        val encoded = preferences.getString(CIPHERTEXT_KEY, null) ?: return null
        val iv = preferences.getString(IV_KEY, null) ?: return null
        decrypted?.takeIf { it.ciphertext == encoded && it.iv == iv }?.let { return it.key }
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
            )
            cipher.doFinal(Base64.decode(encoded, Base64.NO_WRAP)).toString(Charsets.UTF_8)
        }.getOrNull()?.also { decrypted = Decrypted(encoded, iv, it) }
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
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance("AES", "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private class Decrypted(
        val ciphertext: String,
        val iv: String,
        val key: String,
    )

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val CIPHERTEXT_KEY = "api-key-ciphertext"
        const val IV_KEY = "api-key-iv"
    }
}
