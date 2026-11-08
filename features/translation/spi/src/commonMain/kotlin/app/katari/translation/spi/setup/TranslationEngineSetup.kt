package app.katari.translation.spi.setup

import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.host.TranslationSetupDestination
import app.katari.translation.api.model.TranslationModelId
import app.katari.translation.api.model.TranslationModelOperationResult
import app.katari.translation.api.provider.TranslationProviderDisclosure

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
