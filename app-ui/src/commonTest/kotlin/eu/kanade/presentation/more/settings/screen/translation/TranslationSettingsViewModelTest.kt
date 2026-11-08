package eu.kanade.presentation.more.settings.screen.translation

import androidx.lifecycle.ViewModelStore
import app.katari.language.api.tag.LanguageTag
import app.katari.translation.api.TranslationFeature
import app.katari.translation.api.availability.TranslationDeviceAvailability
import app.katari.translation.api.engine.KnownTranslationEngine
import app.katari.translation.api.engine.TranslationEngineArtwork
import app.katari.translation.api.engine.TranslationEngineBuildAvailability
import app.katari.translation.api.engine.TranslationEngineDetails
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.engine.TranslationEngineInspection
import app.katari.translation.api.engine.TranslationEngineSelection
import app.katari.translation.api.engine.TranslationEngineState
import app.katari.translation.api.engine.TranslationEngineStatus
import app.katari.translation.api.engine.TranslationProviderId
import app.katari.translation.api.host.TranslationHostActionResult
import app.katari.translation.api.host.TranslationHostActions
import app.katari.translation.api.language.TranslationDefaultTarget
import app.katari.translation.api.language.TranslationLanguageSupport
import app.katari.translation.api.language.TranslationLanguageSupportInspection
import app.katari.translation.api.model.TranslationModelDescriptor
import app.katari.translation.api.preparation.ReadyTranslation
import app.katari.translation.api.preparation.TranslationEngineChoiceReason
import app.katari.translation.api.preparation.TranslationPreparation
import app.katari.translation.api.preparation.TranslationRoutePreparation
import app.katari.translation.api.preparation.TranslationSystemSetupReason
import app.katari.translation.api.provider.TranslationInvocationPolicy
import app.katari.translation.api.provider.TranslationProviderDisclosure
import app.katari.translation.api.provider.TranslationProviderPresentation
import app.katari.translation.api.request.TranslationBatch
import app.katari.translation.api.request.TranslationRequest
import app.katari.translation.api.request.TranslationRouteRequest
import app.katari.translation.api.request.TranslationTargetLanguageSelection
import app.katari.translation.api.result.TranslationBatchUpdate
import app.katari.translation.api.result.TranslationExecution
import eu.kanade.tachiyomi.ui.base.viewmodel.createViewModel
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
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationSettingsViewModelTest {
    @Test
    fun `provider readiness updates incrementally without resolving selection early`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val inspections = MutableSharedFlow<TranslationEngineInspection>(extraBufferCapacity = 2)
        val hostActions = FakeHostActions().apply {
            inspectionStates = inspections
        }
        val viewModels = ViewModelStore()
        val model = viewModels.createViewModel {
            TranslationSettingsViewModel(
                feature = SetupRequiredFeature(),
                hostActions = hostActions,
            )
        }

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
            viewModels.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `target-only save does not persist the implicit engine`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val hostActions = FakeHostActions()
        val viewModels = ViewModelStore()
        val model = viewModels.createViewModel {
            TranslationSettingsViewModel(
                feature = SetupRequiredFeature(),
                hostActions = hostActions,
            )
        }

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
            viewModels.clear()
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

        override fun translateBatch(batch: TranslationBatch): Flow<TranslationBatchUpdate> =
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
