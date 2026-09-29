package eu.kanade.presentation.more.settings.screen.translation

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.TranslationFeature
import mihon.translation.api.availability.TranslationDeviceAvailability
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineArtwork
import mihon.translation.api.engine.TranslationEngineBuildAvailability
import mihon.translation.api.engine.TranslationEngineDetails
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineInspection
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.engine.TranslationEngineState
import mihon.translation.api.engine.TranslationEngineStatus
import mihon.translation.api.engine.TranslationProviderId
import mihon.translation.api.host.TranslationHostActionResult
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.language.TranslationDefaultTarget
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.preparation.ReadyTranslation
import mihon.translation.api.preparation.TranslationEngineChoiceReason
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationRoutePreparation
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationRouteRequest
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.api.result.TranslationExecution
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationSettingsScreenModelTest {
    @Test
    fun `provider readiness updates incrementally without resolving selection early`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val inspections = MutableSharedFlow<TranslationEngineInspection>(extraBufferCapacity = 2)
        val hostActions = FakeHostActions().apply {
            inspectionStates = inspections
        }
        val model = TranslationSettingsScreenModel(
            feature = SetupRequiredFeature(),
            hostActions = hostActions,
        )

        try {
            runCurrent()
            val partialStates = hostActions.states.map { state ->
                state.copy(
                    status = if (state.engine.id == SECOND_ENGINE) {
                        TranslationEngineStatus.Ready
                    } else {
                        TranslationEngineStatus.Checking
                    },
                )
            }

            inspections.emit(
                TranslationEngineInspection(
                    engines = partialStates,
                    selectedEngine = null,
                    selectionResolved = false,
                ),
            )
            runCurrent()

            model.engines.value shouldBe partialStates
            model.playground.value.engineSelectionResolved shouldBe false
            model.setEngine(SECOND_ENGINE)
            model.playground.value.engine shouldBe SECOND_ENGINE
            model.playground.value.engineSelectionResolved shouldBe true

            inspections.emit(
                TranslationEngineInspection(
                    engines = hostActions.states,
                    selectedEngine = ANDROID_ENGINE,
                ),
            )
            runCurrent()

            model.engines.value shouldBe hostActions.states
            model.playground.value.engineSelectionResolved shouldBe true
            model.playground.value.engine shouldBe SECOND_ENGINE
        } finally {
            model.onDispose()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `target-only save does not persist the implicit engine`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val hostActions = FakeHostActions()
        val model = TranslationSettingsScreenModel(
            feature = SetupRequiredFeature(),
            hostActions = hostActions,
        )

        try {
            advanceUntilIdle()
            model.playground.value.engine shouldBe ANDROID_ENGINE
            hostActions.selectedEngine.isSet() shouldBe false

            model.setSourceLanguage(ENGLISH)
            model.setTargetLanguage(FRENCH)
            model.savePlaygroundDefaults()

            hostActions.selectedEngine.isSet() shouldBe false
            hostActions.defaultTargetLanguage.get() shouldBe
                TranslationTargetLanguageSelection.Explicit(FRENCH)
        } finally {
            model.onDispose()
            Dispatchers.resetMain()
        }
    }

    private class FakeHostActions : TranslationHostActions {
        private val store = InMemoryPreferenceStore()
        override val knownEngines = listOf(knownEngine(ANDROID_ENGINE), knownEngine(SECOND_ENGINE))
        var states = knownEngines.map { engine ->
            TranslationEngineState(
                engine = engine,
                presentation = PRESENTATION,
                status = TranslationEngineStatus.Ready,
            )
        }
        var inspectionStates: Flow<TranslationEngineInspection>? = null
        private val languageSupportByEngine: Map<TranslationEngineId, TranslationLanguageSupportInspection> =
            knownEngines.associate { engine ->
                engine.id to TranslationLanguageSupportInspection.Available(
                    TranslationLanguageSupport.AnyLanguage,
                )
            }
        override val selectedEngine = store.getObjectFromString(
            "engine",
            ANDROID_ENGINE,
            TranslationEngineId::value,
            ::TranslationEngineId,
        )
        override val defaultTargetLanguage:
            tachiyomi.core.common.preference.Preference<TranslationTargetLanguageSelection> =
            store.getObjectFromString(
                "target",
                TranslationTargetLanguageSelection.Default,
                { selection ->
                    (selection as? TranslationTargetLanguageSelection.Explicit)?.language?.value ?: "default"
                },
                { value ->
                    if (value == "default") {
                        TranslationTargetLanguageSelection.Default
                    } else {
                        TranslationTargetLanguageSelection.Explicit(LanguageTag.require(value))
                    }
                },
            )
        override val recentLanguages: tachiyomi.core.common.preference.Preference<List<LanguageTag>> =
            store.getObjectFromString(
                "recent",
                emptyList<LanguageTag>(),
                { languages -> languages.joinToString(",") { it.value } },
                { raw -> raw.split(",").mapNotNull(LanguageTag::parse) },
            )

        override fun defaultTarget(): TranslationDefaultTarget? =
            (defaultTargetLanguage.get() as? TranslationTargetLanguageSelection.Explicit)
                ?.let { TranslationDefaultTarget(it.language, followsAppLanguage = false) }

        override fun recordRecentLanguage(language: LanguageTag) {
            recentLanguages.set(listOf(language) + recentLanguages.get().filterNot { it == language })
        }

        override suspend fun deviceAvailability() = TranslationDeviceAvailability.Available

        override suspend fun inspectEngines() = TranslationEngineInspection(
            engines = states,
            selectedEngine = ANDROID_ENGINE,
        )

        override fun inspectEngineStates(): Flow<TranslationEngineInspection> =
            inspectionStates ?: flow { emit(inspectEngines()) }

        override suspend fun inspectLanguageSupport(engine: TranslationEngineId) =
            languageSupportByEngine.getValue(engine)

        override suspend fun acknowledgeProviderDisclosure(
            engine: TranslationEngineId,
            disclosure: TranslationProviderDisclosure,
        ) = TranslationHostActionResult.Completed

        override suspend fun downloadModels(
            engine: TranslationEngineId,
            models: List<TranslationModelDescriptor>,
            allowMeteredNetwork: Boolean,
        ) = TranslationHostActionResult.ModelsReady

        override fun supportsSetup(engine: TranslationEngineId) = engine == ANDROID_ENGINE

        override suspend fun openSetup(engine: TranslationEngineId) =
            TranslationHostActionResult.SetupUnsupported

        override fun setSelectedEngine(engine: TranslationEngineId) {
            selectedEngine.set(engine)
        }

        override fun setDefaultTargetLanguage(language: LanguageTag?) {
            defaultTargetLanguage.set(
                language?.let(TranslationTargetLanguageSelection::Explicit)
                    ?: TranslationTargetLanguageSelection.Default,
            )
        }
    }

    private class SetupRequiredFeature : TranslationFeature {
        override suspend fun prepare(request: TranslationRequest): TranslationPreparation {
            val engine = (request.engine as? TranslationEngineSelection.Explicit)?.engine
                ?: return TranslationPreparation.EngineChoiceRequired(
                    reason = TranslationEngineChoiceReason.NoEngineConfigured,
                    engines = emptyList(),
                )
            return TranslationPreparation.SystemSetupRequired(
                engine = engine,
                presentation = PRESENTATION,
                reason = TranslationSystemSetupReason.LanguageModelsRequired,
            )
        }

        override suspend fun prepareRoute(route: TranslationRouteRequest): TranslationRoutePreparation =
            error("Settings prepare text")

        override suspend fun translate(ready: ReadyTranslation): TranslationExecution =
            error("Playground must not execute before the user action")
    }

    private companion object {
        val ANDROID_ENGINE = TranslationEngineId("android-system")
        val SECOND_ENGINE = TranslationEngineId("second")

        val ENGLISH = LanguageTag.require("en")
        val FRENCH = LanguageTag.require("fr")
        val PRESENTATION = TranslationProviderPresentation(
            providerId = TranslationProviderId("android"),
            providerName = "Android",
            engineName = "System on-device translation",
            invocationPolicy = TranslationInvocationPolicy.Immediate,
        )

        fun knownEngine(id: TranslationEngineId) = KnownTranslationEngine(
            id = id,
            providerId = PRESENTATION.providerId,
            providerName = PRESENTATION.providerName,
            engineName = PRESENTATION.engineName,
            buildAvailability = TranslationEngineBuildAvailability.Included,
            artwork = TranslationEngineArtwork.Bundled(1),
            details = TranslationEngineDetails(
                description = "Test engine description",
                processingLocation = "Test processing location",
                privacyDescription = "Test privacy description",
            ),
        )
    }
}
