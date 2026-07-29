package mihon.translation.api

/**
 * Request-independent availability of the profile-selected Translation engine.
 *
 * Language detection, language-pair support, and model readiness are intentionally excluded. They can only be
 * inspected authoritatively after a real [TranslationRequest] exists.
 */
sealed interface TranslationDeviceAvailability {
    data object Available : TranslationDeviceAvailability

    data object EngineNotConfigured : TranslationDeviceAvailability

    data class SelectedEngineMissing(
        val engine: TranslationEngineId,
    ) : TranslationDeviceAvailability

    data class SelectedEngineUnavailable(
        val engine: TranslationEngineId,
        val reason: String? = null,
    ) : TranslationDeviceAvailability

    data class UnsupportedOs(
        val minimumApi: Int,
    ) : TranslationDeviceAvailability {
        init {
            require(minimumApi > 0)
        }
    }

    data object TranslationServiceMissing : TranslationDeviceAvailability

    data class ProviderFailure(
        val engine: TranslationEngineId,
        val reason: String,
    ) : TranslationDeviceAvailability {
        init {
            require(reason.isNotBlank())
        }
    }
}
