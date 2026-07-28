package mihon.translation.spi

import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.ResolvedTranslationRequest
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationModelDescriptor
import mihon.translation.api.TranslationOperationProgress
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.api.TranslationProviderPresentation
import mihon.translation.api.TranslationSystemSetupReason
import mihon.translation.api.TranslationUnavailableReason

/**
 * Internal provider adapter. Platform and future engines implement this contract without becoming public API.
 */
interface TranslationEngine {
    val catalogEntry: KnownTranslationEngine
    val presentation: TranslationProviderPresentation
    val maximumInputCodePoints: Int?
    val automaticSelectionPriority: TranslationAutomaticSelectionPriority
        get() = TranslationAutomaticSelectionPriority()

    suspend fun prepare(request: ResolvedTranslationRequest): TranslationEnginePreparation

    suspend fun revalidate(ready: ReadyTranslationEngineRequest): TranslationEnginePreparation

    suspend fun translate(ready: ReadyTranslationEngineRequest): TranslationEngineExecution
}

data class TranslationAutomaticSelectionPriority(
    val ready: Int = 0,
    val setup: Int = 0,
)

/** Provider-owned opaque preparation state. Only the engine that produced it may execute it. */
interface ReadyTranslationEngineRequest

sealed interface TranslationEnginePreparation {
    data class Ready(
        val request: ReadyTranslationEngineRequest,
    ) : TranslationEnginePreparation

    data class ProviderDisclosureRequired(
        val disclosure: TranslationProviderDisclosure,
    ) : TranslationEnginePreparation

    data class ModelDownloadRequired(
        val models: List<TranslationModelDescriptor>,
    ) : TranslationEnginePreparation {
        init {
            require(models.isNotEmpty())
        }
    }

    data class SystemSetupRequired(
        val reason: TranslationSystemSetupReason,
    ) : TranslationEnginePreparation

    data class SetupInProgress(
        val progress: TranslationOperationProgress? = null,
    ) : TranslationEnginePreparation

    data class Unavailable(
        val reason: TranslationUnavailableReason,
    ) : TranslationEnginePreparation
}

sealed interface TranslationEngineExecution {
    data class Success(
        val translatedText: String,
    ) : TranslationEngineExecution {
        init {
            require(translatedText.isNotBlank())
        }
    }

    data class PreparationChanged(
        val preparation: TranslationEnginePreparation,
    ) : TranslationEngineExecution

    data class Failed(
        val message: String? = null,
    ) : TranslationEngineExecution
}

interface TranslationEngineRegistry {
    val engines: List<TranslationEngine>

    fun find(engine: TranslationEngineId): TranslationEngine?
}

interface KnownTranslationEngineCatalog {
    val knownEngines: List<KnownTranslationEngine>
}
