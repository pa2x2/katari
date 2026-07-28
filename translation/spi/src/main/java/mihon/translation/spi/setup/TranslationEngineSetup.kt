package mihon.translation.spi

import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationModelId
import mihon.translation.api.TranslationModelOperationResult
import mihon.translation.api.TranslationProviderDisclosure

interface TranslationEngineSetup {
    val engine: TranslationEngineId

    suspend fun acknowledge(disclosure: TranslationProviderDisclosure)

    suspend fun openSystemSetup(): TranslationSystemSetupResult

    suspend fun downloadModels(
        models: Set<TranslationModelId>,
        allowMeteredNetwork: Boolean,
    ): TranslationModelOperationResult
}

sealed interface TranslationSystemSetupResult {
    data object Opened : TranslationSystemSetupResult

    data object Unsupported : TranslationSystemSetupResult

    data class Failed(
        val reason: String,
    ) : TranslationSystemSetupResult {
        init {
            require(reason.isNotBlank())
        }
    }
}
