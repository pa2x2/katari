package mihon.tts.runtime.playback

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.engine.KnownTtsEngine
import mihon.tts.api.engine.TtsEngineArtwork
import mihon.tts.api.engine.TtsEngineBuildAvailability
import mihon.tts.api.engine.TtsEngineDetails
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsProviderId
import mihon.tts.api.playback.TtsPlaybackFailureReason
import mihon.tts.api.playback.TtsPlaybackStart
import mihon.tts.api.playback.TtsPlaybackState
import mihon.tts.api.playback.TtsStopReason
import mihon.tts.api.playback.TtsStopResult
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.preparation.TtsUnavailableReason
import mihon.tts.api.provider.TtsInputLimit
import mihon.tts.api.provider.TtsOptionalCapability
import mihon.tts.api.provider.TtsParameterSupport
import mihon.tts.api.provider.TtsProviderCapabilities
import mihon.tts.api.provider.TtsProviderPresentation
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.request.ResolvedTtsRequest
import mihon.tts.api.request.TtsParameters
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.api.voice.TtsVoiceInspection
import mihon.tts.runtime.audio.TtsAudioFocus
import mihon.tts.runtime.request.TtsTextSegment
import mihon.tts.spi.engine.ReadyTtsEngineRequest
import mihon.tts.spi.engine.TtsEngine
import mihon.tts.spi.engine.TtsEngineDeviceAvailability
import mihon.tts.spi.engine.TtsEngineExecution
import mihon.tts.spi.engine.TtsEnginePlayback
import mihon.tts.spi.engine.TtsEnginePlaybackEvent
import mihon.tts.spi.engine.TtsEnginePreparation
import mihon.tts.spi.engine.TtsEngineStopResult
import org.junit.jupiter.api.Test

class TtsPlaybackCoordinatorTest {

    @Test
    fun `completion and provider failure release focus and publish terminal state`() = runBlocking<Unit> {
        val completedPlayback = RecordingPlayback()
        val failedPlayback = RecordingPlayback()
        val engine = RecordingEngine(ArrayDeque(listOf(completedPlayback, failedPlayback)))
        val focus = RecordingAudioFocus()
        val coordinator = coordinator(engine, focus)

        val completedSession = coordinator.play(ready(engine))
            .shouldBeInstanceOf<TtsPlaybackStart.Started>()
            .session
        completedPlayback.emit(TtsEnginePlaybackEvent.Started)
        settle()
        completedSession.state.value shouldBe TtsPlaybackState.Speaking()
        completedPlayback.emit(TtsEnginePlaybackEvent.Completed)
        settle()
        completedSession.state.value shouldBe TtsPlaybackState.Completed
        focus.abandonCount shouldBe 1

        val failedSession = coordinator.play(ready(engine))
            .shouldBeInstanceOf<TtsPlaybackStart.Started>()
            .session
        failedPlayback.emit(TtsEnginePlaybackEvent.Failed("provider failed"))
        settle()
        failedSession.state.value shouldBe TtsPlaybackState.Failed(
            TtsPlaybackFailureReason.ProviderFailure(ENGINE_ID, "provider failed"),
        )
        focus.abandonCount shouldBe 2
    }

    @Test
    fun `replacement and stale stop cannot affect the current session`() = runBlocking<Unit> {
        val firstPlayback = RecordingPlayback()
        val secondPlayback = RecordingPlayback()
        val engine = RecordingEngine(ArrayDeque(listOf(firstPlayback, secondPlayback)))
        val focus = RecordingAudioFocus()
        val coordinator = coordinator(engine, focus)

        val firstSession = coordinator.play(ready(engine))
            .shouldBeInstanceOf<TtsPlaybackStart.Started>()
            .session
        firstPlayback.emit(TtsEnginePlaybackEvent.Started)
        settle()

        val secondSession = coordinator.play(ready(engine))
            .shouldBeInstanceOf<TtsPlaybackStart.Started>()
            .session
        firstPlayback.stopCount shouldBe 1
        firstSession.state.value shouldBe TtsPlaybackState.Stopped(TtsStopReason.Replaced)
        firstSession.stop() shouldBe TtsStopResult.AlreadyTerminal

        secondPlayback.emit(TtsEnginePlaybackEvent.Started)
        settle()
        secondSession.state.value shouldBe TtsPlaybackState.Speaking()
        secondSession.stop() shouldBe TtsStopResult.Stopped
        secondPlayback.stopCount shouldBe 1
        secondSession.state.value shouldBe TtsPlaybackState.Stopped(TtsStopReason.Requested)
    }

    @Test
    fun `removed engine during revalidation never requests focus or playback`() = runBlocking<Unit> {
        val engine = RecordingEngine(ArrayDeque(listOf(RecordingPlayback()))).apply {
            revalidation = TtsEnginePreparation.Unavailable(
                TtsUnavailableReason.EngineUnavailable(ENGINE_ID, "Engine removed"),
            )
        }
        val focus = RecordingAudioFocus()
        val coordinator = coordinator(engine, focus)

        coordinator.play(ready(engine)) shouldBe TtsPlaybackStart.PreparationChanged(
            TtsPreparation.Unavailable(TtsUnavailableReason.EngineUnavailable(ENGINE_ID, "Engine removed")),
        )
        focus.requestCount shouldBe 0
        engine.playCount shouldBe 0
    }

    private fun kotlinx.coroutines.CoroutineScope.coordinator(
        engine: RecordingEngine,
        focus: RecordingAudioFocus,
    ) = TtsPlaybackCoordinator(
        scope = this,
        audioFocus = focus,
        mapPreparation = { _, preparation ->
            val unavailable = preparation as TtsEnginePreparation.Unavailable
            TtsPreparation.Unavailable(unavailable.reason)
        },
    )

    private suspend fun settle() {
        repeat(3) { yield() }
    }

    private fun ready(engine: RecordingEngine) = RuntimeReadyTts(
        owner = this,
        engine = engine,
        request = REQUEST,
        segments = listOf(TtsTextSegment(REQUEST.text, 0)),
        firstProviderRequest = PROVIDER_READY,
    )

    private class RecordingEngine(
        private val playbacks: ArrayDeque<RecordingPlayback>,
    ) : TtsEngine {
        override val catalogEntry = ENGINE
        override val presentation = PRESENTATION
        override val capabilities = CAPABILITIES
        var revalidation: TtsEnginePreparation = TtsEnginePreparation.Ready(PROVIDER_READY)
        var playCount = 0
            private set

        override suspend fun inspectDevice() = TtsEngineDeviceAvailability.Available

        override suspend fun inspectVoices() = TtsVoiceInspection.Available(ENGINE_ID, listOf(VOICE), VOICE.id)

        override suspend fun prepare(request: ResolvedTtsRequest) = TtsEnginePreparation.Ready(PROVIDER_READY)

        override suspend fun revalidate(ready: ReadyTtsEngineRequest) = revalidation

        override suspend fun play(ready: ReadyTtsEngineRequest): TtsEngineExecution {
            playCount += 1
            return TtsEngineExecution.Started(playbacks.removeFirst())
        }
    }

    private class RecordingPlayback : TtsEnginePlayback {
        private val channel = Channel<TtsEnginePlaybackEvent>(Channel.UNLIMITED)
        override val events: Flow<TtsEnginePlaybackEvent> = channel.receiveAsFlow()
        var stopCount = 0
            private set

        suspend fun emit(event: TtsEnginePlaybackEvent) {
            channel.send(event)
        }

        override suspend fun stop(): TtsEngineStopResult {
            stopCount += 1
            return TtsEngineStopResult.Stopped
        }
    }

    private class RecordingAudioFocus : TtsAudioFocus {
        private var held = false
        var requestCount = 0
            private set
        var abandonCount = 0
            private set

        override fun request(): Boolean {
            requestCount += 1
            held = true
            return true
        }

        override fun abandon() {
            if (!held) return
            held = false
            abandonCount += 1
        }
    }

    private companion object {
        val LANGUAGE = LanguageTag.require("en-US")
        val PROVIDER_ID = TtsProviderId("test")
        val ENGINE_ID = TtsEngineId("test-engine")
        val ENGINE = KnownTtsEngine(
            id = ENGINE_ID,
            providerId = PROVIDER_ID,
            providerName = "Test provider",
            engineName = "Test engine",
            buildAvailability = TtsEngineBuildAvailability.Included,
            artwork = TtsEngineArtwork.Bundled(1),
            details = TtsEngineDetails("Test engine", "Test processing", "Test privacy"),
        )
        val PRESENTATION = TtsProviderPresentation(PROVIDER_ID, "Test provider", "Test engine")
        val CAPABILITIES = TtsProviderCapabilities(
            rangeProgress = TtsOptionalCapability.Unsupported,
            speechRate = TtsParameterSupport.Unsupported,
            pitch = TtsParameterSupport.Unsupported,
            inputLimit = TtsInputLimit.Unspecified,
        )
        val VOICE = TtsVoice(
            id = TtsVoiceId(PROVIDER_ID, ENGINE_ID, "voice"),
            name = "Voice",
            language = LANGUAGE,
            processing = TtsVoiceProcessing.OnDevice,
        )
        val REQUEST = ResolvedTtsRequest(
            text = "Text",
            language = LANGUAGE,
            engine = ENGINE_ID,
            voice = VOICE,
            parameters = TtsParameters(),
            networkProcessingAllowed = false,
        )
        val PROVIDER_READY = object : ReadyTtsEngineRequest {}
    }
}
