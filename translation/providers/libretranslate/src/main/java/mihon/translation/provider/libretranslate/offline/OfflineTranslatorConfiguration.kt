package mihon.translation.provider.libretranslate.offline

import android.content.Context
import okhttp3.HttpUrl

internal interface OfflineTranslatorSettings {
    var port: Int
    var disclosureAccepted: Boolean

    fun endpoint(): HttpUrl
}

internal class OfflineTranslatorConfiguration(
    context: Context,
) : OfflineTranslatorSettings {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override var port: Int
        get() = preferences.getInt(PORT_KEY, DEFAULT_PORT).takeIf(::isValidPort) ?: DEFAULT_PORT
        set(value) {
            require(isValidPort(value)) { "Offline Translator port must be in 1..65535" }
            preferences.edit().putInt(PORT_KEY, value).apply()
        }

    override var disclosureAccepted: Boolean
        get() = preferences.getBoolean(DISCLOSURE_ACCEPTED_KEY, false)
        set(value) {
            preferences.edit().putBoolean(DISCLOSURE_ACCEPTED_KEY, value).apply()
        }

    override fun endpoint(): HttpUrl {
        return HttpUrl.Builder()
            .scheme("http")
            .host(LOOPBACK_HOST)
            .port(port)
            .build()
    }

    companion object {
        const val DEFAULT_PORT = 5000
        const val LOOPBACK_HOST = "127.0.0.1"

        fun parsePort(value: String): Int? = value.trim().toIntOrNull()?.takeIf(::isValidPort)

        private fun isValidPort(value: Int): Boolean = value in 1..65535

        private const val PREFERENCES_NAME = "translation.offline-translator"
        private const val PORT_KEY = "http-api-port"
        private const val DISCLOSURE_ACCEPTED_KEY = "provider-disclosure-accepted"
    }
}
