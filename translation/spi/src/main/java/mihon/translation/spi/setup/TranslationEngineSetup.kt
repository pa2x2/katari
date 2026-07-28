package mihon.translation.spi

import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationModelId
import mihon.translation.api.TranslationModelOperationResult
import mihon.translation.api.TranslationProviderDisclosure

interface TranslationEngineSetup {
    val engine: TranslationEngineId
    val supportsSystemSetup: Boolean
        get() = false

    suspend fun acknowledge(disclosure: TranslationProviderDisclosure)

    suspend fun openSystemSetup(): TranslationSystemSetupResult

    suspend fun downloadModels(
        models: Set<TranslationModelId>,
        allowMeteredNetwork: Boolean,
    ): TranslationModelOperationResult
}

interface TranslationEngineSetupRegistry {
    fun find(engine: TranslationEngineId): TranslationEngineSetup?
}

sealed interface TranslationSystemSetupResult {
    data object Opened : TranslationSystemSetupResult

    data object Unsupported : TranslationSystemSetupResult

    data object ServiceMissing : TranslationSystemSetupResult

    data object SettingsUnavailable : TranslationSystemSetupResult

    data class Failed(
        val reason: String,
    ) : TranslationSystemSetupResult {
        init {
            require(reason.isNotBlank())
        }
    }
}
