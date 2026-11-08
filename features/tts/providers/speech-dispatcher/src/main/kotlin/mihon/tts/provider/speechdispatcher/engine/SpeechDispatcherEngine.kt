package mihon.tts.provider.speechdispatcher.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mihon.tts.api.engine.KnownTtsEngine
import mihon.tts.api.preparation.TtsSystemSetupReason
import mihon.tts.api.preparation.TtsUnavailableReason
import mihon.tts.api.provider.TtsInputLimit
import mihon.tts.api.provider.TtsOptionalCapability
import mihon.tts.api.provider.TtsParameterSupport
import mihon.tts.api.provider.TtsProviderCapabilities
import mihon.tts.api.provider.TtsProviderPresentation
import mihon.tts.api.request.ResolvedTtsRequest
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.api.voice.TtsVoiceInspection
import mihon.tts.provider.speechdispatcher.availability.SpeechDispatcherProblem
import mihon.tts.provider.speechdispatcher.connection.SpeechDispatcherConnection
import mihon.tts.provider.speechdispatcher.protocol.SsipReply
import mihon.tts.provider.speechdispatcher.service.SpeechDispatcherAddress
import mihon.tts.provider.speechdispatcher.service.SpeechDispatcherConnecting
import mihon.tts.provider.speechdispatcher.service.SpeechDispatcherLauncher
import mihon.tts.provider.speechdispatcher.service.SpeechDispatcherService
import mihon.tts.provider.speechdispatcher.service.SpeechDispatcherSession
import mihon.tts.provider.speechdispatcher.voice.SpeechDispatcherVoice
import mihon.tts.provider.speechdispatcher.voice.parseSynthesisVoices
import mihon.tts.provider.speechdispatcher.voice.speechDispatcherVoiceId
import mihon.tts.provider.speechdispatcher.voice.toApiVoice
import mihon.tts.spi.engine.ReadyTtsEngineRequest
import mihon.tts.spi.engine.TtsEngine
import mihon.tts.spi.engine.TtsEngineDeviceAvailability
import mihon.tts.spi.engine.TtsEngineExecution
import mihon.tts.spi.engine.TtsEnginePreparation
import mihon.tts.spi.engine.TtsEngineStopResult
import java.io.IOException

/**
 * Every voice of every module Speech Dispatcher has, as one engine. Commands that belong together, such as choosing a
 * module and listing its voices, run as one step so that two of them never mix their settings on the shared
 * connection.
 */
internal class SpeechDispatcherEngine(
    override val catalogEntry: KnownTtsEngine,
    override val presentation: TtsProviderPresentation,
    address: SpeechDispatcherAddress,
    launcher: SpeechDispatcherLauncher,
    clientName: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TtsEngine {
    private val messages = SpeechDispatcherMessages()
    private val service = SpeechDispatcherService(
        address = address,
        launcher = launcher,
        clientName = clientName,
        onEvent = messages::receive,
        onDisconnected = ::disconnected,
    )
    private val stepMutex = Mutex()

    @Volatile
    private var catalog: VoiceCatalog? = null

    override val capabilities = TtsProviderCapabilities(
        rangeProgress = TtsOptionalCapability.Unsupported,
        speechRate = TtsParameterSupport.Supported(SPEECH_DISPATCHER_PARAMETER_RANGE),
        pitch = TtsParameterSupport.Supported(SPEECH_DISPATCHER_PARAMETER_RANGE),
        inputLimit = TtsInputLimit.Unspecified,
    )

    override suspend fun inspectDevice(): TtsEngineDeviceAvailability = withContext(dispatcher) {
        when (val voices = voiceCatalog(forceRefresh = false)) {
            is CatalogResult.Listed -> TtsEngineDeviceAvailability.Available
            is CatalogResult.Unavailable -> when (val problem = voices.problem) {
                SpeechDispatcherProblem.NotInstalled -> TtsEngineDeviceAvailability.NotInstalled
                SpeechDispatcherProblem.NoVoices -> TtsEngineDeviceAvailability.VoiceDataRequired(problem.message)
                else -> TtsEngineDeviceAvailability.Unavailable(problem.message)
            }
        }
    }

    override suspend fun inspectVoices(): TtsVoiceInspection = withContext(dispatcher) {
        voiceInspection(forceRefresh = false)
    }

    override suspend fun refreshVoices(): TtsVoiceInspection = withContext(dispatcher) {
        voiceInspection(forceRefresh = true)
    }

    override suspend fun prepare(request: ResolvedTtsRequest): TtsEnginePreparation = withContext(dispatcher) {
        readiness(request)
    }

    override suspend fun revalidate(ready: ReadyTtsEngineRequest): TtsEnginePreparation {
        val speechReady = (ready as? SpeechDispatcherReadyRequest)
            ?.takeIf { it.owner === this }
            ?: return unavailable(SpeechDispatcherProblem.StoppedResponding)
        return withContext(dispatcher) { readiness(speechReady.request) }
    }

    override suspend fun play(ready: ReadyTtsEngineRequest): TtsEngineExecution {
        val speechReady = (ready as? SpeechDispatcherReadyRequest)
            ?.takeIf { it.owner === this }
            ?: return TtsEngineExecution.Failed(SpeechDispatcherProblem.StoppedResponding.message)
        return withContext(dispatcher) { speak(speechReady) }
    }

    override suspend fun release() = withContext(dispatcher) {
        catalog = null
        service.close()
    }

    private suspend fun speak(speechReady: SpeechDispatcherReadyRequest): TtsEngineExecution {
        return try {
            when (val connecting = service.connect()) {
                is SpeechDispatcherConnecting.Unavailable -> TtsEngineExecution.Failed(connecting.problem.message)
                is SpeechDispatcherConnecting.Connected -> stepMutex.withLock {
                    queue(connecting.session.connection, speechReady.request, speechReady.voice)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            TtsEngineExecution.Failed(SpeechDispatcherProblem.StoppedResponding.message)
        }
    }

    private suspend fun queue(
        connection: SpeechDispatcherConnection,
        request: ResolvedTtsRequest,
        voice: SpeechDispatcherVoice,
    ): TtsEngineExecution {
        listOf(
            "SET self OUTPUT_MODULE ${voice.module}",
            "SET self LANGUAGE ${voice.language}",
            "SET self SYNTHESIS_VOICE ${voice.name}",
            "SET self RATE ${speechDispatcherStep(request.parameters.speechRate)}",
            "SET self PITCH ${speechDispatcherStep(request.parameters.pitch)}",
            "SET self SSML_MODE off",
        ).forEach { command ->
            connection.command(command).refusal()?.let { return TtsEngineExecution.Failed(it.message) }
        }
        val queued = connection.speak(request.text)
        queued.refusal()?.let { return TtsEngineExecution.Failed(it.message) }
        val messageId = queued.data.firstOrNull()?.toLongOrNull()
            ?: return TtsEngineExecution.Failed(SpeechDispatcherProblem.Refused(queued.statusLine).message)
        val playback = SpeechDispatcherPlayback(cancel = ::cancel)
        messages.add(messageId, playback)
        return TtsEngineExecution.Started(playback)
    }

    private suspend fun cancel(playback: SpeechDispatcherPlayback): TtsEngineStopResult = withContext(dispatcher) {
        cancelSpeech(playback)
    }

    private suspend fun cancelSpeech(playback: SpeechDispatcherPlayback): TtsEngineStopResult {
        val connection = (service.connect() as? SpeechDispatcherConnecting.Connected)?.session?.connection
            ?: return TtsEngineStopResult.Failed(SpeechDispatcherProblem.StoppedResponding.message)
        return try {
            val reply = connection.command("CANCEL self")
            reply.refusal()?.let { return TtsEngineStopResult.Failed(it.message) }
            messages.remove(playback)
            playback.stopped()
            TtsEngineStopResult.Stopped
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            TtsEngineStopResult.Failed(SpeechDispatcherProblem.StoppedResponding.message)
        }
    }

    private suspend fun readiness(request: ResolvedTtsRequest): TtsEnginePreparation {
        return when (val voices = voiceCatalog(forceRefresh = false)) {
            is CatalogResult.Unavailable -> unavailable(voices.problem)
            is CatalogResult.Listed -> voices.catalog.voicesById[request.voice.id.value]
                ?.let { TtsEnginePreparation.Ready(SpeechDispatcherReadyRequest(this, request, it)) }
                ?: TtsEnginePreparation.SystemSetupRequired(TtsSystemSetupReason.VoiceDataRequired)
        }
    }

    private suspend fun voiceInspection(forceRefresh: Boolean): TtsVoiceInspection {
        return when (val voices = voiceCatalog(forceRefresh)) {
            is CatalogResult.Listed -> TtsVoiceInspection.Available(
                engine = catalogEntry.id,
                voices = voices.catalog.voices,
                defaultVoice = voices.catalog.defaultVoice,
            )
            is CatalogResult.Unavailable -> TtsVoiceInspection.Unavailable(catalogEntry.id, voices.problem.message)
        }
    }

    private suspend fun voiceCatalog(forceRefresh: Boolean): CatalogResult {
        if (!forceRefresh) catalog?.let { return CatalogResult.Listed(it) }
        val session = when (val connecting = service.connect()) {
            is SpeechDispatcherConnecting.Unavailable -> return CatalogResult.Unavailable(connecting.problem)
            is SpeechDispatcherConnecting.Connected -> connecting.session
        }
        return try {
            stepMutex.withLock {
                if (!forceRefresh) catalog?.let { return@withLock CatalogResult.Listed(it) }
                listVoices(session)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            CatalogResult.Unavailable(SpeechDispatcherProblem.StoppedResponding)
        }
    }

    private suspend fun listVoices(session: SpeechDispatcherSession): CatalogResult {
        val connection = session.connection
        val modules = connection.command("LIST OUTPUT_MODULES")
        modules.refusal()?.let { return CatalogResult.Unavailable(it) }
        val voices = modules.data.flatMap { module ->
            connection.command("SET self OUTPUT_MODULE $module").refusal()?.let { return CatalogResult.Unavailable(it) }
            val listed = connection.command("LIST SYNTHESIS_VOICES")
            listed.refusal()?.let { return CatalogResult.Unavailable(it) }
            parseSynthesisVoices(module, listed.data)
        }
        val listed = catalogOf(voices, session)
            ?: return CatalogResult.Unavailable(SpeechDispatcherProblem.NoVoices)
        catalog = listed
        return CatalogResult.Listed(listed)
    }

    private fun catalogOf(
        voices: List<SpeechDispatcherVoice>,
        session: SpeechDispatcherSession,
    ): VoiceCatalog? {
        val namesModule = voices.distinctBy(SpeechDispatcherVoice::module).size > 1
        val listed = voices.mapNotNull { voice ->
            voice.toApiVoice(catalogEntry.providerId, catalogEntry.id, namesModule)?.let { it to voice }
        }
        if (listed.isEmpty()) return null
        // The voice the server would speak its own language in, unless Katari picks another.
        val defaultVoice = voices
            .filter { it.module == session.defaultModule }
            .let { moduleVoices ->
                moduleVoices.firstOrNull { it.language.equals(session.defaultLanguage, ignoreCase = true) }
                    ?: moduleVoices.firstOrNull {
                        it.language.substringBefore('-') == session.defaultLanguage?.substringBefore('-')
                    }
            }
            ?.let { speechDispatcherVoiceId(catalogEntry.providerId, catalogEntry.id, it) }
        return VoiceCatalog(
            voices = listed.map { it.first }.sortedBy { it.name },
            voicesById = listed.associate { (api, voice) -> api.id.value to voice },
            defaultVoice = defaultVoice?.takeIf { id -> listed.any { it.first.id == id } },
        )
    }

    private fun disconnected() {
        catalog = null
        messages.failAll(SpeechDispatcherProblem.StoppedResponding.message)
    }

    private fun unavailable(problem: SpeechDispatcherProblem): TtsEnginePreparation =
        TtsEnginePreparation.Unavailable(TtsUnavailableReason.EngineUnavailable(catalogEntry.id, problem.message))

    private fun SsipReply.refusal(): SpeechDispatcherProblem? =
        if (isSuccess) null else SpeechDispatcherProblem.Refused(statusLine)
}

private data class VoiceCatalog(
    val voices: List<TtsVoice>,
    val voicesById: Map<String, SpeechDispatcherVoice>,
    val defaultVoice: TtsVoiceId?,
)

private sealed interface CatalogResult {
    data class Listed(val catalog: VoiceCatalog) : CatalogResult

    data class Unavailable(val problem: SpeechDispatcherProblem) : CatalogResult
}

private data class SpeechDispatcherReadyRequest(
    val owner: SpeechDispatcherEngine,
    val request: ResolvedTtsRequest,
    val voice: SpeechDispatcherVoice,
) : ReadyTtsEngineRequest
