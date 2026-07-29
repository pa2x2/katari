package mihon.translation.spi

import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationModelId
import mihon.translation.api.TranslationModelOperationResult
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.api.TranslationSetupDestination

interface TranslationEngineSetup {
    val engine: TranslationEngineId
    val supportsSetup: Boolean
        get() = false

    suspend fun acknowledge(disclosure: TranslationProviderDisclosure)

    suspend fun openSetup(): TranslationSetupResult

    suspend fun downloadModels(
        models: Set<TranslationModelId>,
        allowMeteredNetwork: Boolean,
    ): TranslationModelOperationResult
}

interface TranslationEngineSetupRegistry {
    fun findSetup(engine: TranslationEngineId): TranslationEngineSetup?
}

sealed interface TranslationSetupResult {
    data class Opened(
        val destination: TranslationSetupDestination,
    ) : TranslationSetupResult

    data object Unsupported : TranslationSetupResult

    data object ServiceMissing : TranslationSetupResult

    data object SettingsUnavailable : TranslationSetupResult

    data class Failed(
        val reason: String,
    ) : TranslationSetupResult {
        init {
            require(reason.isNotBlank())
        }
    }
}
