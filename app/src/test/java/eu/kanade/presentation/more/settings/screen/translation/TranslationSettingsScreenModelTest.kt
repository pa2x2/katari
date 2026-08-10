package eu.kanade.presentation.more.settings.screen.translation

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
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
import mihon.translation.api.host.TranslationSetupDestination
import mihon.translation.api.language.TranslationLanguagePair
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.preparation.ReadyTranslation
import mihon.translation.api.preparation.TranslationEngineChoiceReason
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.api.result.TranslationExecution
import mihon.translation.ui.session.TranslationSessionState
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
    fun `engine cannot be selected before its readiness requirement is satisfied`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val hostActions = FakeHostActions().apply {
            states = knownEngines.map { engine ->
                TranslationEngineState(
                    engine = engine,
                    presentation = PRESENTATION,
                    status = if (engine.id == SECOND_ENGINE) {
                        TranslationEngineStatus.NotInstalled
                    } else {
                        TranslationEngineStatus.Ready
                    },
                )
            }
        }
        val model = TranslationSettingsScreenModel(
            feature = SetupRequiredFeature(),
            hostActions = hostActions,
        )

        try {
            advanceUntilIdle()
            model.setEngine(SECOND_ENGINE)

            model.playground.value.engine shouldBe ANDROID_ENGINE
            model.playground.value.hasUnsavedProfileChanges shouldBe false
        } finally {
            model.onDispose()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `playground stages profile settings until save while request-only edits stay transient`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val hostActions = FakeHostActions()
        hostActions.defaultTargetLanguage.set(TranslationTargetLanguageSelection.Explicit(ENGLISH))
        val feature = SetupRequiredFeature(hostActions.knownEngines)
        val model = TranslationSettingsScreenModel(
            feature = feature,
            hostActions = hostActions,
        )

        try {
            advanceUntilIdle()

            feature.lastRequest shouldBe TranslationRequest(
                text = "Bonjour tout le monde",
                sourceLanguage = TranslationSourceLanguageSelection.Explicit(FRENCH),
                targetLanguage = TranslationTargetLanguageSelection.Explicit(ENGLISH),
                engine = TranslationEngineSelection.Explicit(ANDROID_ENGINE),
            )
            model.controller.state.value
                .shouldBeInstanceOf<TranslationSessionState.PreparationRequired>()
            model.playground.value.hasUnsavedProfileChanges shouldBe false
            model.supportsSetup(ANDROID_ENGINE) shouldBe true
            model.supportsSetup(SECOND_ENGINE) shouldBe false

            model.setSourceLanguage(ENGLISH)
            model.setText("A request-only experiment")
            advanceUntilIdle()
            model.playground.value.hasUnsavedProfileChanges shouldBe false

            model.setTargetLanguage(FRENCH)
            model.setEngine(SECOND_ENGINE)
            advanceUntilIdle()
            model.playground.value.hasUnsavedProfileChanges shouldBe true
            hostActions.selectedEngine.get() shouldBe ANDROID_ENGINE
            hostActions.defaultTargetLanguage.get() shouldBe
                TranslationTargetLanguageSelection.Explicit(ENGLISH)

            model.savePlaygroundDefaults()

            hostActions.selectedEngine.get() shouldBe SECOND_ENGINE
            hostActions.defaultTargetLanguage.get() shouldBe
                TranslationTargetLanguageSelection.Explicit(FRENCH)
            model.playground.value.hasUnsavedProfileChanges shouldBe false
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

    @Test
    fun `engine switch preserves an unsupported pair until the user chooses a valid pair`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val hostActions = FakeHostActions().apply {
            defaultTargetLanguage.set(TranslationTargetLanguageSelection.Explicit(FRENCH))
            languageSupportByEngine = mapOf(
                ANDROID_ENGINE to exactSupport(ENGLISH, FRENCH),
                SECOND_ENGINE to exactSupport(FRENCH, ENGLISH),
            )
        }
        val model = TranslationSettingsScreenModel(
            feature = SetupRequiredFeature(),
            hostActions = hostActions,
        )

        try {
            advanceUntilIdle()
            model.setEngine(SECOND_ENGINE)
            advanceUntilIdle()

            model.playground.value.sourceLanguage shouldBe ENGLISH
            model.playground.value.targetLanguage shouldBe FRENCH
            model.savePlaygroundDefaults()
            hostActions.selectedEngine.isSet() shouldBe false

            model.setSourceLanguage(FRENCH)
            model.setTargetLanguage(ENGLISH)
            model.savePlaygroundDefaults()

            hostActions.selectedEngine.get() shouldBe SECOND_ENGINE
            hostActions.defaultTargetLanguage.get() shouldBe
                TranslationTargetLanguageSelection.Explicit(ENGLISH)
        } finally {
            model.onDispose()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `unavailable implicit engine leaves the playground unconfigured`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val hostActions = FakeHostActions().apply {
            inspectedSelection = null
            states = states.map { state ->
                if (state.engine.id == ANDROID_ENGINE) {
                    state.copy(
                        status = TranslationEngineStatus.Unavailable(TranslationUnavailableReason.ServiceMissing),
                    )
                } else {
                    state
                }
            }
        }
        val feature = SetupRequiredFeature(hostActions.knownEngines)
        val model = TranslationSettingsScreenModel(
            feature = feature,
            hostActions = hostActions,
        )

        try {
            advanceUntilIdle()

            model.playground.value.engine shouldBe null
            model.playground.value.engineSelectionResolved shouldBe true
            feature.lastRequest?.engine shouldBe TranslationEngineSelection.ProfileDefault
            model.controller.state.value
                .shouldBeInstanceOf<TranslationSessionState.PreparationRequired>()
                .preparation shouldBe TranslationPreparation.EngineChoiceRequired(
                reason = TranslationEngineChoiceReason.NoEngineConfigured,
                engines = hostActions.knownEngines,
            )
        } finally {
            model.onDispose()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `disposing the playground discards unsaved profile changes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val hostActions = FakeHostActions()
        hostActions.defaultTargetLanguage.set(TranslationTargetLanguageSelection.Explicit(ENGLISH))
        val model = TranslationSettingsScreenModel(
            feature = SetupRequiredFeature(),
            hostActions = hostActions,
        )

        try {
            advanceUntilIdle()
            model.setTargetLanguage(FRENCH)
            model.setEngine(SECOND_ENGINE)
            model.playground.value.hasUnsavedProfileChanges shouldBe true

            model.onDispose()

            hostActions.selectedEngine.get() shouldBe ANDROID_ENGINE
            hostActions.defaultTargetLanguage.get() shouldBe
                TranslationTargetLanguageSelection.Explicit(ENGLISH)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `returning from either setup destination retries the playground once`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            TranslationSetupDestination.entries.forEach { destination ->
                val setupResult = TranslationHostActionResult.SetupOpened(destination)
                val hostActions = FakeHostActions().apply {
                    this.setupResult = setupResult
                }
                val feature = SetupRequiredFeature()
                val model = TranslationSettingsScreenModel(
                    feature = feature,
                    hostActions = hostActions,
                )
                advanceUntilIdle()
                val requestsBeforeSetup = feature.requestCount
                var completedResult: TranslationHostActionResult? = null

                model.openSetup(ANDROID_ENGINE) { completedResult = it }
                advanceUntilIdle()

                completedResult shouldBe setupResult
                feature.requestCount shouldBe requestsBeforeSetup

                model.onResume()
                advanceUntilIdle()
                feature.requestCount shouldBe requestsBeforeSetup + 1

                model.onResume()
                advanceUntilIdle()
                feature.requestCount shouldBe requestsBeforeSetup + 1
                model.onDispose()
            }
        } finally {
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
        var inspectedSelection: TranslationEngineId? = ANDROID_ENGINE
        var inspectionStates: Flow<TranslationEngineInspection>? = null
        var setupResult: TranslationHostActionResult = TranslationHostActionResult.SetupUnsupported
        var languageSupportByEngine: Map<TranslationEngineId, TranslationLanguageSupportInspection> =
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

        override suspend fun deviceAvailability() = TranslationDeviceAvailability.Available

        override suspend fun inspectEngines() = TranslationEngineInspection(
            engines = states,
            selectedEngine = inspectedSelection,
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

        override suspend fun openSetup(engine: TranslationEngineId) = setupResult

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

    private class SetupRequiredFeature(
        private val engines: List<KnownTranslationEngine> = emptyList(),
    ) : TranslationFeature {
        var lastRequest: TranslationRequest? = null
        var requestCount = 0

        override suspend fun prepare(request: TranslationRequest): TranslationPreparation {
            requestCount += 1
            lastRequest = request
            val engine = (request.engine as? TranslationEngineSelection.Explicit)?.engine
                ?: return TranslationPreparation.EngineChoiceRequired(
                    reason = TranslationEngineChoiceReason.NoEngineConfigured,
                    engines = engines,
                )
            return TranslationPreparation.SystemSetupRequired(
                engine = engine,
                presentation = PRESENTATION,
                reason = TranslationSystemSetupReason.LanguageModelsRequired,
            )
        }

        override suspend fun translate(ready: ReadyTranslation): TranslationExecution =
            error("Playground must not execute before the user action")
    }

    private companion object {
        val ANDROID_ENGINE = TranslationEngineId("android-system")
        val SECOND_ENGINE = TranslationEngineId("second")

        fun exactSupport(
            source: LanguageTag,
            target: LanguageTag,
        ) = TranslationLanguageSupportInspection.Available(
            TranslationLanguageSupport.ExactPairs(
                setOf(TranslationLanguagePair(source, target)),
            ),
        )
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
