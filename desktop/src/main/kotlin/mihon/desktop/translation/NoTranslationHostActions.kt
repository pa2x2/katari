package mihon.desktop.translation

import dev.zacsweers.metro.Inject
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.availability.TranslationDeviceAvailability
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineInspection
import mihon.translation.api.host.TranslationHostActionResult
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.language.TranslationDefaultTarget
import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.api.request.TranslationTargetLanguageSelection
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference
import tachiyomi.core.common.preference.Preference

/**
 * Translation settings with no engine to set up: [knownEngines] is empty, which is how a platform reports that it
 * doesn't translate. Nothing can be chosen, so the translation settings keep nothing. Stands in until desktop has a
 * translation engine.
 */
@Inject
class NoTranslationHostActions : TranslationHostActions {
    override val knownEngines: List<KnownTranslationEngine> = emptyList()
    override val selectedEngine: Preference<TranslationEngineId> =
        InMemoryPreference("translation_engine", null, NO_ENGINE)
    override val defaultTargetLanguage: Preference<TranslationTargetLanguageSelection> =
        InMemoryPreference("translation_target_language", null, TranslationTargetLanguageSelection.Default)
    override val recentLanguages: Preference<List<LanguageTag>> =
        InMemoryPreference("translation_recent_languages", null, emptyList())

    override fun recordRecentLanguage(language: LanguageTag) = Unit

    override fun defaultTarget(): TranslationDefaultTarget? = null

    override suspend fun deviceAvailability(): TranslationDeviceAvailability =
        TranslationDeviceAvailability.EngineNotConfigured

    override suspend fun inspectEngines(): TranslationEngineInspection =
        TranslationEngineInspection(engines = emptyList(), selectedEngine = null)

    override suspend fun inspectLanguageSupport(engine: TranslationEngineId): TranslationLanguageSupportInspection =
        TranslationLanguageSupportInspection.Unavailable()

    override suspend fun acknowledgeProviderDisclosure(
        engine: TranslationEngineId,
        disclosure: TranslationProviderDisclosure,
    ): TranslationHostActionResult = TranslationHostActionResult.SetupUnsupported

    override suspend fun downloadModels(
        engine: TranslationEngineId,
        models: List<TranslationModelDescriptor>,
        allowMeteredNetwork: Boolean,
    ): TranslationHostActionResult = TranslationHostActionResult.SetupUnsupported

    override fun supportsSetup(engine: TranslationEngineId): Boolean = false

    override suspend fun openSetup(engine: TranslationEngineId): TranslationHostActionResult =
        TranslationHostActionResult.SetupUnsupported

    override fun setSelectedEngine(engine: TranslationEngineId) = Unit

    override fun setDefaultTargetLanguage(language: LanguageTag?) = Unit

    private companion object {
        val NO_ENGINE = TranslationEngineId("translation-unavailable")
    }
}
