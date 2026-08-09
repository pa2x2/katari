package mihon.entry.interactions.book.reader.selection

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mihon.tts.api.TtsFeature
import mihon.tts.api.playback.TtsPlaybackSession
import mihon.tts.api.playback.TtsPlaybackStart
import mihon.tts.api.playback.TtsPlaybackState
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.request.TtsRequest

internal enum class BookSelectionSpeechPhase {
    Idle,
    Preparing,
    Speaking,
}

internal data class BookSelectionSpeechState(
    val phase: BookSelectionSpeechPhase = BookSelectionSpeechPhase.Idle,
    val selectionIdentity: String? = null,
)

internal enum class BookSelectionSpeechFailure {
    LanguageUnavailable,
    ConfigurationRequired,
    Unavailable,
    PlaybackFailed,
}

internal class BookSelectionSpeechController(
    private val feature: TtsFeature,
    private val scope: CoroutineScope,
    private val onFailure: (BookSelectionSpeechFailure) -> Unit,
) : AutoCloseable {
    private val mutableState = MutableStateFlow(BookSelectionSpeechState())
    val state: StateFlow<BookSelectionSpeechState> = mutableState.asStateFlow()

    private var job: Job? = null
    private var session: TtsPlaybackSession? = null
    private var generation = 0L
    private var closed = false

    fun toggle(selection: BookReaderTextSelection) {
        if (closed) return
        if (
            mutableState.value.phase != BookSelectionSpeechPhase.Idle &&
            mutableState.value.selectionIdentity == selection.identity
        ) {
            stop()
        } else {
            start(selection)
        }
    }

    fun onSelectionChanged(selectionIdentity: String) {
        val state = mutableState.value
        if (state.phase != BookSelectionSpeechPhase.Idle && state.selectionIdentity != selectionIdentity) {
            stop()
        }
    }

    fun clearSelection(ownerSelectionIdentity: String? = null) {
        val activeIdentity = mutableState.value.selectionIdentity ?: return
        if (ownerSelectionIdentity == null || activeIdentity == ownerSelectionIdentity) stop()
    }

    fun stopPlayback() {
        if (mutableState.value.phase != BookSelectionSpeechPhase.Idle) stop()
    }

    override fun close() {
        if (closed) return
        stop()
        closed = true
    }

    private fun start(selection: BookReaderTextSelection) {
        stop()
        val activeGeneration = ++generation
        mutableState.value = BookSelectionSpeechState(
            phase = BookSelectionSpeechPhase.Preparing,
            selectionIdentity = selection.identity,
        )
        job = scope.launch {
            try {
                when (val preparation = feature.prepare(TtsRequest(text = selection.text))) {
                    is TtsPreparation.Ready -> play(preparation, selection.identity, activeGeneration)
                    else -> fail(activeGeneration, preparation.failure())
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                fail(activeGeneration, BookSelectionSpeechFailure.PlaybackFailed)
            }
        }
    }

    private suspend fun play(
        preparation: TtsPreparation.Ready,
        selectionIdentity: String,
        activeGeneration: Long,
    ) {
        when (val start = feature.play(preparation.speech)) {
            is TtsPlaybackStart.Started -> observe(start.session, selectionIdentity, activeGeneration)
            is TtsPlaybackStart.PreparationChanged -> fail(activeGeneration, start.preparation.failure())
            is TtsPlaybackStart.Failed -> fail(activeGeneration, BookSelectionSpeechFailure.PlaybackFailed)
        }
    }

    private suspend fun observe(
        activeSession: TtsPlaybackSession,
        selectionIdentity: String,
        activeGeneration: Long,
    ) {
        if (activeGeneration != generation || closed) {
            activeSession.stop()
            return
        }
        session = activeSession
        activeSession.state.first { playback ->
            when (playback) {
                TtsPlaybackState.Starting -> false
                is TtsPlaybackState.Speaking -> {
                    update(activeGeneration, BookSelectionSpeechPhase.Speaking, selectionIdentity)
                    false
                }
                TtsPlaybackState.Completed,
                is TtsPlaybackState.Stopped,
                -> finish(activeGeneration)
                is TtsPlaybackState.Failed -> fail(
                    activeGeneration,
                    BookSelectionSpeechFailure.PlaybackFailed,
                )
            }
        }
    }

    private fun stop() {
        generation += 1
        job?.cancel()
        job = null
        val activeSession = session
        session = null
        mutableState.value = BookSelectionSpeechState()
        if (activeSession != null) scope.launch { activeSession.stop() }
    }

    private fun update(
        activeGeneration: Long,
        phase: BookSelectionSpeechPhase,
        selectionIdentity: String,
    ) {
        if (activeGeneration == generation && !closed) {
            mutableState.value = BookSelectionSpeechState(phase, selectionIdentity)
        }
    }

    private fun finish(activeGeneration: Long): Boolean {
        if (activeGeneration == generation && !closed) {
            job = null
            session = null
            mutableState.value = BookSelectionSpeechState()
        }
        return true
    }

    private fun fail(activeGeneration: Long, failure: BookSelectionSpeechFailure): Boolean {
        if (activeGeneration == generation && !closed) {
            job = null
            session = null
            mutableState.value = BookSelectionSpeechState()
            onFailure(failure)
        }
        return true
    }
}

private fun TtsPreparation.failure(): BookSelectionSpeechFailure = when (this) {
    is TtsPreparation.LanguageChoiceRequired -> BookSelectionSpeechFailure.LanguageUnavailable
    is TtsPreparation.EngineChoiceRequired,
    is TtsPreparation.ProviderDisclosureRequired,
    is TtsPreparation.SystemSetupRequired,
    is TtsPreparation.VoiceChoiceRequired,
    -> BookSelectionSpeechFailure.ConfigurationRequired
    is TtsPreparation.Rejected,
    is TtsPreparation.Unavailable,
    -> BookSelectionSpeechFailure.Unavailable
    is TtsPreparation.Ready -> error("Ready speech is not a failure")
}
