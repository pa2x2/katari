package mihon.translation.api.host

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.availability.TranslationDeviceAvailability
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineInspection
import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.api.request.TranslationTargetLanguageSelection
import tachiyomi.core.common.preference.Preference

/**
 * Provider-neutral host boundary shared by settings and reader surfaces.
 *
 * Provider adapters and setup registries remain internal to `translation:runtime`.
 */
interface TranslationHostActions {
    val knownEngines: List<KnownTranslationEngine>

    /**
     * Raw profile preference. [Preference.isSet] distinguishes an explicit choice from the implicit device default.
     */
    val selectedEngine: Preference<TranslationEngineId>
    val defaultTargetLanguage: Preference<TranslationTargetLanguageSelection>

    suspend fun deviceAvailability(): TranslationDeviceAvailability

    suspend fun inspectEngines(): TranslationEngineInspection

    /**
     * Emits inspection progress beginning with the engines that are still being checked.
     *
     * Hosts with independently inspectable providers should override this to publish each provider result as it
     * arrives. The default preserves compatibility with hosts that only support an aggregate inspection.
     */
    fun inspectEngineStates(): Flow<TranslationEngineInspection> = flow {
        emit(inspectEngines())
    }

    suspend fun inspectLanguageSupport(
        engine: TranslationEngineId,
    ): TranslationLanguageSupportInspection

    suspend fun acknowledgeProviderDisclosure(
        engine: TranslationEngineId,
        disclosure: TranslationProviderDisclosure,
    ): TranslationHostActionResult

    suspend fun downloadModels(
        engine: TranslationEngineId,
        models: List<TranslationModelDescriptor>,
        allowMeteredNetwork: Boolean = false,
    ): TranslationHostActionResult

    fun supportsSetup(engine: TranslationEngineId): Boolean

    suspend fun openSetup(engine: TranslationEngineId): TranslationHostActionResult

    fun setSelectedEngine(engine: TranslationEngineId)

    fun setDefaultTargetLanguage(language: LanguageTag?)
}

sealed interface TranslationHostActionResult {
    data object Completed : TranslationHostActionResult

    data object ModelsReady : TranslationHostActionResult

    data class ModelsFailed(
        val reason: String,
    ) : TranslationHostActionResult {
        init {
            require(reason.isNotBlank())
        }
    }

    data class SetupOpened(
        val destination: TranslationSetupDestination,
    ) : TranslationHostActionResult

    data object SetupUnsupported : TranslationHostActionResult

    data object ServiceMissing : TranslationHostActionResult

    data object SettingsUnavailable : TranslationHostActionResult

    data class Failed(
        val reason: String,
    ) : TranslationHostActionResult {
        init {
            require(reason.isNotBlank())
        }
    }
}

enum class TranslationSetupDestination {
    InApp,
    External,
}
