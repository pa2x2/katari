package eu.kanade.tachiyomi.ui.translator.session

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import mihon.translation.api.language.TranslationLanguagePair
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.preparation.ReadyTranslation
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.request.ResolvedTranslationRequest
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.api.result.TranslationExecution
import mihon.translation.api.result.TranslationFailureReason
import mihon.translation.api.result.TranslationResult
import mihon.translation.ui.session.TranslationSessionState
import mihon.translation.ui.session.displayedSessionResult
import mihon.tts.api.TtsFeature
import mihon.tts.api.playback.TtsPlaybackStart
import mihon.tts.api.preparation.ReadyTts
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.request.TtsRequest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

@OptIn(ExperimentalCoroutinesApi::class)
class TranslatorScreenModelTest {
    @Test
    fun `live translation waits 200 milliseconds and only translates the latest text`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val feature = FakeTranslationFeature()
        val model = TranslatorScreenModel(
            feature = feature,
            hostActions = FakeHostActions(),
            ttsFeature = FakeTtsFeature,
        )

        try {
            runCurrent()
            model.setText("hel")
            advanceTimeBy(100)
            model.setText("hello")
            advanceTimeBy(199)
            runCurrent()
            feature.requests shouldBe emptyList()

            advanceTimeBy(1)
            runCurrent()
            feature.requests.map { it.text } shouldBe listOf("hello")
        } finally {
            model.onDispose()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `clearing text clears its result and preserves all session controls`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val hostActions = FakeHostActions()
        val model = TranslatorScreenModel(
            feature = FakeTranslationFeature(),
            hostActions = hostActions,
            ttsFeature = FakeTtsFeature,
        )

        try {
            advanceUntilIdle()
            model.selectSource(FRENCH)
            model.selectTarget(ENGLISH)
            model.selectEngine(SECOND_ENGINE_ID)
            model.setText("bonjour")
            advanceUntilIdle()

            model.state.value.session.displayedSessionResult()?.result?.translatedText shouldBe "hello"
            model.clearText()

            with(model.state.value) {
                text shouldBe ""
                session.displayedSessionResult() shouldBe null
                sourceLanguage shouldBe TranslationSourceLanguageSelection.Explicit(FRENCH)
                targetLanguage shouldBe TranslationTargetLanguageSelection.Explicit(ENGLISH)
                engine shouldBe TranslationEngineSelection.Explicit(SECOND_ENGINE_ID)
            }
            hostActions.selectedEngine.get() shouldBe ENGINE_ID
            hostActions.defaultTargetLanguage.get() shouldBe TranslationTargetLanguageSelection.Explicit(FRENCH)
        } finally {
            model.onDispose()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `swap promotes the successful result and translates the resolved pair back`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val model = TranslatorScreenModel(
            feature = FakeTranslationFeature(),
            hostActions = FakeHostActions(),
            ttsFeature = FakeTtsFeature,
        )

        try {
            advanceUntilIdle()
            model.setText("hello")
            advanceUntilIdle()
            model.swapLanguages()

            with(model.state.value) {
                text shouldBe "bonjour"
                sourceLanguage shouldBe TranslationSourceLanguageSelection.Explicit(FRENCH)
                targetLanguage shouldBe TranslationTargetLanguageSelection.Explicit(ENGLISH)
            }

            advanceUntilIdle()
            model.state.value.session.displayedSessionResult()?.result?.translatedText shouldBe "hello"
        } finally {
            model.onDispose()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `failed replacement keeps the previous successful result available`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val model = TranslatorScreenModel(
            feature = FakeTranslationFeature(),
            hostActions = FakeHostActions(),
            ttsFeature = FakeTtsFeature,
        )

        try {
            advanceUntilIdle()
            model.setText("hello")
            advanceUntilIdle()
            val successful = model.state.value.session.displayedSessionResult()

            model.setText("fail")
            advanceUntilIdle()

            model.state.value.session.displayedSessionResult() shouldBe successful
            model.state.value.session.shouldBeInstanceOf<TranslationSessionState.Failed>()
        } finally {
            model.onDispose()
            Dispatchers.resetMain()
        }
    }

    private class FakeTranslationFeature : TranslationFeature {
        val requests = mutableListOf<TranslationRequest>()

        override suspend fun prepare(request: TranslationRequest): TranslationPreparation {
            requests += request
            val source = when (val selection = request.sourceLanguage) {
                TranslationSourceLanguageSelection.Automatic -> if (request.text == "bonjour") FRENCH else ENGLISH
                is TranslationSourceLanguageSelection.Explicit -> selection.language
            }
            val target = when (val selection = request.targetLanguage) {
                TranslationTargetLanguageSelection.Default -> FRENCH
                is TranslationTargetLanguageSelection.Explicit -> selection.language
            }
            val engine = (request.engine as? TranslationEngineSelection.Explicit)?.engine ?: ENGINE_ID
            val resolved = ResolvedTranslationRequest(request.text, source, target, engine)
            return TranslationPreparation.Ready(
                translation = FakeReadyTranslation(resolved),
                request = resolved,
                presentation = PRESENTATION,
            )
        }

        override suspend fun translate(ready: ReadyTranslation): TranslationExecution {
            val request = (ready as FakeReadyTranslation).request
            if (request.text == "fail") {
                return TranslationExecution.Failed(
                    TranslationFailureReason.ProviderFailure(request.engine, null),
                )
            }
            return TranslationExecution.Success(
                TranslationResult(
                    translatedText = if (request.text == "bonjour") "hello" else "bonjour",
                    sourceLanguage = request.sourceLanguage,
                    targetLanguage = request.targetLanguage,
                    presentation = PRESENTATION,
                ),
            )
        }
    }

    private data class FakeReadyTranslation(val request: ResolvedTranslationRequest) : ReadyTranslation

    private class FakeHostActions : TranslationHostActions {
        private val store = InMemoryPreferenceStore()
        override val knownEngines = listOf(knownEngine(ENGINE_ID), knownEngine(SECOND_ENGINE_ID))
        private val engineStates = knownEngines.map {
            TranslationEngineState(
                engine = it,
                presentation = PRESENTATION,
                status = TranslationEngineStatus.Ready,
            )
        }
        override val selectedEngine = store.getObjectFromString(
            "engine",
            ENGINE_ID,
            TranslationEngineId::value,
            ::TranslationEngineId,
        )
        override val defaultTargetLanguage:
            tachiyomi.core.common.preference.Preference<TranslationTargetLanguageSelection> =
            store.getObjectFromString(
                "target",
                TranslationTargetLanguageSelection.Explicit(FRENCH),
                { (it as TranslationTargetLanguageSelection.Explicit).language.value },
                { TranslationTargetLanguageSelection.Explicit(LanguageTag.require(it)) },
            )

        override suspend fun deviceAvailability() = TranslationDeviceAvailability.Available

        override suspend fun inspectEngines() = TranslationEngineInspection(engineStates, ENGINE_ID)

        override fun inspectEngineStates() = flow { emit(inspectEngines()) }

        override suspend fun inspectLanguageSupport(engine: TranslationEngineId) =
            TranslationLanguageSupportInspection.Available(
                TranslationLanguageSupport.ExactPairs(
                    setOf(
                        TranslationLanguagePair(ENGLISH, FRENCH),
                        TranslationLanguagePair(FRENCH, ENGLISH),
                    ),
                ),
            )

        override suspend fun acknowledgeProviderDisclosure(
            engine: TranslationEngineId,
            disclosure: TranslationProviderDisclosure,
        ) = TranslationHostActionResult.Completed

        override suspend fun downloadModels(
            engine: TranslationEngineId,
            models: List<TranslationModelDescriptor>,
            allowMeteredNetwork: Boolean,
        ) = TranslationHostActionResult.ModelsReady

        override fun supportsSetup(engine: TranslationEngineId) = false

        override suspend fun openSetup(engine: TranslationEngineId) = TranslationHostActionResult.SetupUnsupported

        override fun setSelectedEngine(engine: TranslationEngineId) {
            selectedEngine.set(engine)
        }

        override fun setDefaultTargetLanguage(language: LanguageTag?) {
            defaultTargetLanguage.set(
                TranslationTargetLanguageSelection.Explicit(language ?: FRENCH),
            )
        }

        private fun knownEngine(id: TranslationEngineId) = KnownTranslationEngine(
            id = id,
            providerId = PRESENTATION.providerId,
            providerName = PRESENTATION.providerName,
            engineName = "${PRESENTATION.engineName} ${id.value}",
            buildAvailability = TranslationEngineBuildAvailability.Included,
            artwork = TranslationEngineArtwork.Bundled(1),
            details = TranslationEngineDetails(
                description = "Test engine",
                processingLocation = "On device",
                privacyDescription = "Test privacy",
            ),
        )
    }

    private data object FakeTtsFeature : TtsFeature {
        override suspend fun prepare(request: TtsRequest): TtsPreparation =
            error("Speech is not used by these tests")

        override suspend fun play(ready: ReadyTts): TtsPlaybackStart =
            error("Speech is not used by these tests")
    }

    private companion object {
        val ENGINE_ID = TranslationEngineId("test")
        val SECOND_ENGINE_ID = TranslationEngineId("second")
        val ENGLISH = LanguageTag.require("en")
        val FRENCH = LanguageTag.require("fr")
        val PRESENTATION = TranslationProviderPresentation(
            providerId = TranslationProviderId("test"),
            providerName = "Test",
            engineName = "Test engine",
            invocationPolicy = TranslationInvocationPolicy.Immediate,
        )
    }
}
