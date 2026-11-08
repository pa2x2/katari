package mihon.translation.runtime.system

import mihon.language.api.tag.LanguageTag
import mihon.translation.api.language.TranslationLanguageSupportInspection

internal data class AndroidSystemTranslationPair(
    val source: LanguageTag,
    val target: LanguageTag,
)

internal enum class AndroidSystemCapabilityState {
    OnDevice,
    AvailableToDownload,
    Downloading,
    Unavailable,
}

internal sealed interface AndroidSystemTranslationInspection {
    data object UnsupportedOs : AndroidSystemTranslationInspection

    data object ServiceMissing : AndroidSystemTranslationInspection

    data class Capability(
        val state: AndroidSystemCapabilityState,
    ) : AndroidSystemTranslationInspection

    data object UnsupportedPair : AndroidSystemTranslationInspection

    data class Failed(
        val reason: String,
    ) : AndroidSystemTranslationInspection
}

internal sealed interface AndroidSystemDeviceInspection {
    data object Available : AndroidSystemDeviceInspection

    data object UnsupportedOs : AndroidSystemDeviceInspection

    data object ServiceMissing : AndroidSystemDeviceInspection

    data class Failed(
        val reason: String,
    ) : AndroidSystemDeviceInspection
}

internal sealed interface AndroidSystemPlatformExecution {
    data class Success(
        val translatedText: String,
    ) : AndroidSystemPlatformExecution

    data class CapabilityChanged(
        val inspection: AndroidSystemTranslationInspection,
    ) : AndroidSystemPlatformExecution

    data object ContextUnsupported : AndroidSystemPlatformExecution

    /**
     * The service declined to open a translator for a pair it had just reported as on-device.
     * Android gives no reason; in practice its installed language data no longer satisfies the
     * service, as after an Android System Intelligence update moved to newer models.
     */
    data object TranslatorRefused : AndroidSystemPlatformExecution

    data class Failed(
        val reason: String,
    ) : AndroidSystemPlatformExecution
}

internal sealed interface AndroidSystemPlatformSetup {
    data object Opened : AndroidSystemPlatformSetup

    data object ServiceMissing : AndroidSystemPlatformSetup

    data object SettingsUnavailable : AndroidSystemPlatformSetup

    data class Failed(
        val reason: String,
    ) : AndroidSystemPlatformSetup
}

internal interface AndroidSystemTranslationPlatform {
    suspend fun inspectDevice(): AndroidSystemDeviceInspection

    suspend fun inspectLanguageSupport(): TranslationLanguageSupportInspection

    suspend fun inspect(pair: AndroidSystemTranslationPair): AndroidSystemTranslationInspection

    suspend fun translate(
        pair: AndroidSystemTranslationPair,
        text: String,
    ): AndroidSystemPlatformExecution

    suspend fun openSettings(): AndroidSystemPlatformSetup
}
