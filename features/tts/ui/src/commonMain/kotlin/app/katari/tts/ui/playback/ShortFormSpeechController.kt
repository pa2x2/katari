package app.katari.tts.ui.playback

import app.katari.language.api.identification.TextLanguageResolutionContext
import app.katari.language.api.tag.LanguageTag
import app.katari.tts.api.TtsFeature
import app.katari.tts.api.playback.TtsPlaybackSession
import app.katari.tts.api.playback.TtsPlaybackStart
import app.katari.tts.api.playback.TtsPlaybackState
import app.katari.tts.api.preparation.TtsPreparation
import app.katari.tts.api.request.TtsLanguageSelection
import app.katari.tts.api.request.TtsRequest
import app.katari.tts.ui.engine.engineExplanation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ShortFormSpeechPhase {
    Idle,
    Preparing,
    Speaking,
}

data class ShortFormSpeechRequest<Owner : Any>(
    val owner: Owner,
    val text: String,
    val language: TtsLanguageSelection,
    val languageContext: TextLanguageResolutionContext = TextLanguageResolutionContext(),
) {
    init {
        require(text.isNotBlank())
    }
}

data class ShortFormSpeechState<Owner : Any>(
    val phase: ShortFormSpeechPhase = ShortFormSpeechPhase.Idle,
    val owner: Owner? = null,
) {
    init {
        require((phase == ShortFormSpeechPhase.Idle) == (owner == null))
    }
}

/** Why speech didn't play. */
sealed interface ShortFormSpeechFailure {
    /** The engine's own words for the user, which a surface shows in place of its general message; null without. */
    val explanation: String?
        get() = null

    data object LanguageUnavailable : ShortFormSpeechFailure

    data object ConfigurationRequired : ShortFormSpeechFailure

    data class Unavailable(override val explanation: String?) : ShortFormSpeechFailure

    data class PlaybackFailed(override val explanation: String?) : ShortFormSpeechFailure
}

class ShortFormSpeechController<Owner : Any>(
    private val feature: TtsFeature,
    private val scope: CoroutineScope,
    private val onFailure: (ShortFormSpeechFailure) -> Unit,
    private val onLanguageResolved: (LanguageTag) -> Unit = {},
) : AutoCloseable {
    private val mutableState = MutableStateFlow(ShortFormSpeechState<Owner>())
    val state: StateFlow<ShortFormSpeechState<Owner>> = mutableState.asStateFlow()

    private var job: Job? = null
    private var session: TtsPlaybackSession? = null
    private var generation = 0L
    private var closed = false

    fun toggle(request: ShortFormSpeechRequest<Owner>) {
        if (closed) return
        if (mutableState.value.phase != ShortFormSpeechPhase.Idle && mutableState.value.owner == request.owner) {
            stop()
        } else {
            start(request)
        }
    }

    fun stopIfOwnerChanged(validOwners: Set<Owner>) {
        val activeOwner = mutableState.value.owner ?: return
        if (activeOwner !in validOwners) stop()
    }

    fun stopPlayback() {
        if (mutableState.value.phase != ShortFormSpeechPhase.Idle) stop()
    }

    override fun close() {
        if (closed) return
        stop()
        closed = true
    }

    private fun start(request: ShortFormSpeechRequest<Owner>) {
        stop()
        val activeGeneration = ++generation
        mutableState.value = ShortFormSpeechState(
            phase = ShortFormSpeechPhase.Preparing,
            owner = request.owner,
        )
        job = scope.launch {
            try {
                when (
                    val preparation = feature.prepare(
                        TtsRequest(
                            text = request.text,
                            language = request.language,
                            languageContext = request.languageContext,
                        ),
                    )
                ) {
                    is TtsPreparation.Ready -> play(preparation, request.owner, activeGeneration)
                    else -> fail(activeGeneration, preparation.failure())
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                fail(activeGeneration, ShortFormSpeechFailure.PlaybackFailed(explanation = null))
            }
        }
    }

    private suspend fun play(
        preparation: TtsPreparation.Ready,
        owner: Owner,
        activeGeneration: Long,
    ) {
        if (!isCurrent(activeGeneration)) return
        onLanguageResolved(preparation.request.language)
        when (val start = feature.play(preparation.speech)) {
            is TtsPlaybackStart.Started -> observe(start.session, owner, activeGeneration)
            is TtsPlaybackStart.PreparationChanged -> fail(activeGeneration, start.preparation.failure())
            is TtsPlaybackStart.Failed -> fail(
                activeGeneration,
                ShortFormSpeechFailure.PlaybackFailed(start.reason.engineExplanation()),
            )
        }
    }

    private suspend fun observe(
        activeSession: TtsPlaybackSession,
        owner: Owner,
        activeGeneration: Long,
    ) {
        if (!isCurrent(activeGeneration)) {
            withContext(NonCancellable) { activeSession.stop() }
            return
        }
        session = activeSession
        activeSession.state.first { playback ->
            when (playback) {
                TtsPlaybackState.Starting -> false
                is TtsPlaybackState.Speaking -> {
                    update(activeGeneration, ShortFormSpeechPhase.Speaking, owner)
                    false
                }
                TtsPlaybackState.Completed,
                is TtsPlaybackState.Stopped,
                -> finish(activeGeneration)
                is TtsPlaybackState.Failed -> fail(
                    activeGeneration,
                    ShortFormSpeechFailure.PlaybackFailed(playback.reason.engineExplanation()),
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
        mutableState.value = ShortFormSpeechState()
        if (activeSession != null) {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                withContext(NonCancellable) { activeSession.stop() }
            }
        }
    }

    private fun update(
        activeGeneration: Long,
        phase: ShortFormSpeechPhase,
        owner: Owner,
    ) {
        if (isCurrent(activeGeneration)) {
            mutableState.value = ShortFormSpeechState(phase, owner)
        }
    }

    private fun finish(activeGeneration: Long): Boolean {
        if (isCurrent(activeGeneration)) {
            job = null
            session = null
            mutableState.value = ShortFormSpeechState()
        }
        return true
    }

    private fun fail(activeGeneration: Long, failure: ShortFormSpeechFailure): Boolean {
        if (isCurrent(activeGeneration)) {
            job = null
            session = null
            mutableState.value = ShortFormSpeechState()
            onFailure(failure)
        }
        return true
    }

    private fun isCurrent(activeGeneration: Long): Boolean = activeGeneration == generation && !closed
}

private fun TtsPreparation.failure(): ShortFormSpeechFailure = when (this) {
    is TtsPreparation.LanguageChoiceRequired -> ShortFormSpeechFailure.LanguageUnavailable
    is TtsPreparation.EngineChoiceRequired,
    is TtsPreparation.ProviderDisclosureRequired,
    is TtsPreparation.SystemSetupRequired,
    is TtsPreparation.VoiceChoiceRequired,
    -> ShortFormSpeechFailure.ConfigurationRequired
    is TtsPreparation.Rejected,
    is TtsPreparation.Unavailable,
    -> ShortFormSpeechFailure.Unavailable(engineExplanation())
    is TtsPreparation.Ready -> error("Ready speech is not a failure")
}
