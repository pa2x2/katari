package mihon.tts.ui.settings

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.TtsFeature
import mihon.tts.api.playback.TtsPlaybackSession
import mihon.tts.api.playback.TtsPlaybackStart
import mihon.tts.api.playback.TtsPlaybackState
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.request.TtsLanguageSelection
import mihon.tts.api.request.TtsRequest

internal class TtsPreviewPlaybackController(
    private val feature: TtsFeature,
    private val scope: CoroutineScope,
    private val currentState: () -> TtsPreviewState,
    private val updateState: (TtsPreviewState) -> Unit,
) : AutoCloseable {
    private var job: Job? = null
    private var session: TtsPlaybackSession? = null
    private var generation = 0L
    private var closed = false

    fun toggle(language: LanguageTag) {
        toggle(
            TtsRequest(
                text = previewSample(language).text,
                language = TtsLanguageSelection.Explicit(language),
            ),
        )
    }

    fun toggle(request: TtsRequest) {
        if (closed) return
        when (currentState()) {
            TtsPreviewState.Preparing,
            TtsPreviewState.Speaking,
            -> stop()
            else -> start(request)
        }
    }

    fun play(request: TtsRequest) {
        if (closed) return
        start(request)
    }

    fun stop() {
        generation += 1
        job?.cancel()
        job = null
        val activeSession = session
        session = null
        updateState(TtsPreviewState.Idle)
        if (activeSession != null) {
            scope.launch { activeSession.stop() }
        }
    }

    override fun close() {
        if (closed) return
        stop()
        closed = true
    }

    private fun start(request: TtsRequest) {
        stop()
        val activeGeneration = ++generation
        updateState(TtsPreviewState.Preparing)
        job = scope.launch {
            try {
                when (
                    val preparation = feature.prepare(
                        request,
                    )
                ) {
                    is TtsPreparation.Ready -> play(preparation, activeGeneration)
                    else -> update(activeGeneration, TtsPreviewState.ActionRequired(preparation))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                update(
                    activeGeneration,
                    TtsPreviewState.Failed(reason = null, message = error.message?.takeUnless(String::isBlank)),
                )
            }
        }
    }

    private suspend fun play(preparation: TtsPreparation.Ready, activeGeneration: Long) {
        when (val start = feature.play(preparation.speech)) {
            is TtsPlaybackStart.Started -> observe(start.session, activeGeneration)
            is TtsPlaybackStart.PreparationChanged -> update(
                activeGeneration,
                TtsPreviewState.ActionRequired(start.preparation),
            )
            is TtsPlaybackStart.Failed -> update(
                activeGeneration,
                TtsPreviewState.Failed(start.reason),
            )
        }
    }

    private suspend fun observe(activeSession: TtsPlaybackSession, activeGeneration: Long) {
        if (activeGeneration != generation) {
            activeSession.stop()
            return
        }
        session = activeSession
        activeSession.state.first { playback ->
            when (playback) {
                TtsPlaybackState.Starting -> {
                    update(activeGeneration, TtsPreviewState.Preparing)
                    false
                }
                is TtsPlaybackState.Speaking -> {
                    update(activeGeneration, TtsPreviewState.Speaking)
                    false
                }
                TtsPlaybackState.Completed,
                is TtsPlaybackState.Stopped,
                -> finish(activeGeneration, TtsPreviewState.Idle)
                is TtsPlaybackState.Failed -> finish(
                    activeGeneration,
                    TtsPreviewState.Failed(playback.reason),
                )
            }
        }
    }

    private fun finish(activeGeneration: Long, state: TtsPreviewState): Boolean {
        if (activeGeneration == generation && !closed) {
            updateState(state)
            session = null
        }
        return true
    }

    private fun update(activeGeneration: Long, state: TtsPreviewState) {
        if (activeGeneration == generation && !closed) {
            updateState(state)
        }
    }
}
