package eu.kanade.presentation.more.settings.screen.translation

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.TranslationDeviceAvailability
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationEngineSelection
import mihon.translation.api.TranslationEngineState
import mihon.translation.api.TranslationEngineStatus
import mihon.translation.api.TranslationExecution
import mihon.translation.api.TranslationFeature
import mihon.translation.api.TranslationHostActionResult
import mihon.translation.api.TranslationHostActions
import mihon.translation.api.TranslationInvocationPolicy
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationModelDescriptor
import mihon.translation.api.TranslationPreparation
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.api.TranslationProviderId
import mihon.translation.api.TranslationProviderPresentation
import mihon.translation.api.TranslationRequest
import mihon.translation.api.TranslationSourceLanguageSelection
import mihon.translation.api.TranslationSystemSetupReason
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.ui.session.TranslationSessionState
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class TranslationSettingsScreenModelTest {
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
        val feature = SetupRequiredFeature()
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
                        TranslationTargetLanguageSelection.Explicit(TranslationLanguageTag.require(value))
                    }
                },
            )
        override val automaticSelectionEnabled = store.getBoolean("automatic", false)

        override suspend fun deviceAvailability() = TranslationDeviceAvailability.Available

        override suspend fun inspectEngineStates() = states

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

        override fun setDefaultTargetLanguage(language: TranslationLanguageTag?) {
            defaultTargetLanguage.set(
                language?.let(TranslationTargetLanguageSelection::Explicit)
                    ?: TranslationTargetLanguageSelection.Default,
            )
        }
    }

    private class SetupRequiredFeature : TranslationFeature {
        var lastRequest: TranslationRequest? = null

        override suspend fun prepare(request: TranslationRequest): TranslationPreparation {
            lastRequest = request
            return TranslationPreparation.SystemSetupRequired(
                engine = (request.engine as TranslationEngineSelection.Explicit).engine,
                presentation = PRESENTATION,
                reason = TranslationSystemSetupReason.LanguageModelsRequired,
            )
        }

        override suspend fun translate(
            ready: mihon.translation.api.ReadyTranslation,
        ): TranslationExecution = error("Playground must not execute before the user action")
    }

    private companion object {
        val ANDROID_ENGINE = TranslationEngineId("android-system")
        val SECOND_ENGINE = TranslationEngineId("second")
        val ENGLISH = TranslationLanguageTag.require("en")
        val FRENCH = TranslationLanguageTag.require("fr")
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
        )
    }
}
