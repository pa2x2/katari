package mihon.entry.interactions.book.reader.speech

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mihon.translation.ui.presentation.TranslationResultSpeechTarget
import mihon.tts.api.TtsFeature
import mihon.tts.api.playback.TtsPlaybackSession
import mihon.tts.api.playback.TtsPlaybackStart
import mihon.tts.api.playback.TtsPlaybackState
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.request.TtsLanguageSelection
import mihon.tts.api.request.TtsRequest

internal enum class BookShortFormSpeechPhase {
    Idle,
    Preparing,
    Speaking,
}

internal sealed interface BookShortFormSpeechOwner {
    data class Selection(val identity: String) : BookShortFormSpeechOwner

    data class TranslationResult(
        val target: TranslationResultSpeechTarget,
    ) : BookShortFormSpeechOwner
}

internal data class BookShortFormSpeechRequest(
    val owner: BookShortFormSpeechOwner,
    val text: String,
    val language: TtsLanguageSelection,
) {
    init {
        require(text.isNotBlank())
    }
}

internal data class BookShortFormSpeechState(
    val phase: BookShortFormSpeechPhase = BookShortFormSpeechPhase.Idle,
    val owner: BookShortFormSpeechOwner? = null,
) {
    init {
        require((phase == BookShortFormSpeechPhase.Idle) == (owner == null))
    }
}

internal enum class BookShortFormSpeechFailure {
    LanguageUnavailable,
    ConfigurationRequired,
    Unavailable,
    PlaybackFailed,
}

internal class BookShortFormSpeechController(
    private val feature: TtsFeature,
    private val scope: CoroutineScope,
    private val onFailure: (BookShortFormSpeechFailure) -> Unit,
) : AutoCloseable {
    private val mutableState = MutableStateFlow(BookShortFormSpeechState())
    val state: StateFlow<BookShortFormSpeechState> = mutableState.asStateFlow()

    private var job: Job? = null
    private var session: TtsPlaybackSession? = null
    private var generation = 0L
    private var closed = false

    fun toggle(request: BookShortFormSpeechRequest) {
        if (closed) return
        if (
            mutableState.value.phase != BookShortFormSpeechPhase.Idle &&
            mutableState.value.owner == request.owner
        ) {
            stop()
        } else {
            start(request)
        }
    }

    fun onSelectionChanged(selectionIdentity: String) {
        val owner = mutableState.value.owner as? BookShortFormSpeechOwner.Selection ?: return
        if (owner.identity != selectionIdentity) stop()
    }

    fun clearSelection(selectionIdentity: String? = null) {
        val owner = mutableState.value.owner as? BookShortFormSpeechOwner.Selection ?: return
        if (selectionIdentity == null || owner.identity == selectionIdentity) stop()
    }

    fun clearTranslationResult() {
        if (mutableState.value.owner is BookShortFormSpeechOwner.TranslationResult) stop()
    }

    fun stopPlayback() {
        if (mutableState.value.phase != BookShortFormSpeechPhase.Idle) stop()
    }

    override fun close() {
        if (closed) return
        stop()
        closed = true
    }

    private fun start(request: BookShortFormSpeechRequest) {
        stop()
        val activeGeneration = ++generation
        mutableState.value = BookShortFormSpeechState(
            phase = BookShortFormSpeechPhase.Preparing,
            owner = request.owner,
        )
        job = scope.launch {
            try {
                when (
                    val preparation = feature.prepare(
                        TtsRequest(
                            text = request.text,
                            language = request.language,
                        ),
                    )
                ) {
                    is TtsPreparation.Ready -> play(preparation, request.owner, activeGeneration)
                    else -> fail(activeGeneration, preparation.failure())
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                fail(activeGeneration, BookShortFormSpeechFailure.PlaybackFailed)
            }
        }
    }

    private suspend fun play(
        preparation: TtsPreparation.Ready,
        owner: BookShortFormSpeechOwner,
        activeGeneration: Long,
    ) {
        when (val start = feature.play(preparation.speech)) {
            is TtsPlaybackStart.Started -> observe(start.session, owner, activeGeneration)
            is TtsPlaybackStart.PreparationChanged -> fail(activeGeneration, start.preparation.failure())
            is TtsPlaybackStart.Failed -> fail(activeGeneration, BookShortFormSpeechFailure.PlaybackFailed)
        }
    }

    private suspend fun observe(
        activeSession: TtsPlaybackSession,
        owner: BookShortFormSpeechOwner,
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
                    update(activeGeneration, BookShortFormSpeechPhase.Speaking, owner)
                    false
                }
                TtsPlaybackState.Completed,
                is TtsPlaybackState.Stopped,
                -> finish(activeGeneration)
                is TtsPlaybackState.Failed -> fail(
                    activeGeneration,
                    BookShortFormSpeechFailure.PlaybackFailed,
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
        mutableState.value = BookShortFormSpeechState()
        if (activeSession != null) scope.launch { activeSession.stop() }
    }

    private fun update(
        activeGeneration: Long,
        phase: BookShortFormSpeechPhase,
        owner: BookShortFormSpeechOwner,
    ) {
        if (activeGeneration == generation && !closed) {
            mutableState.value = BookShortFormSpeechState(phase, owner)
        }
    }

    private fun finish(activeGeneration: Long): Boolean {
        if (activeGeneration == generation && !closed) {
            job = null
            session = null
            mutableState.value = BookShortFormSpeechState()
        }
        return true
    }

    private fun fail(activeGeneration: Long, failure: BookShortFormSpeechFailure): Boolean {
        if (activeGeneration == generation && !closed) {
            job = null
            session = null
            mutableState.value = BookShortFormSpeechState()
            onFailure(failure)
        }
        return true
    }
}

private fun TtsPreparation.failure(): BookShortFormSpeechFailure = when (this) {
    is TtsPreparation.LanguageChoiceRequired -> BookShortFormSpeechFailure.LanguageUnavailable
    is TtsPreparation.EngineChoiceRequired,
    is TtsPreparation.ProviderDisclosureRequired,
    is TtsPreparation.SystemSetupRequired,
    is TtsPreparation.VoiceChoiceRequired,
    -> BookShortFormSpeechFailure.ConfigurationRequired
    is TtsPreparation.Rejected,
    is TtsPreparation.Unavailable,
    -> BookShortFormSpeechFailure.Unavailable
    is TtsPreparation.Ready -> error("Ready speech is not a failure")
}
