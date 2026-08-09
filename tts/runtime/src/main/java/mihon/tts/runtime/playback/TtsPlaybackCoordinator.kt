package mihon.tts.runtime.playback

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mihon.tts.api.playback.TtsPlaybackFailureReason
import mihon.tts.api.playback.TtsPlaybackSession
import mihon.tts.api.playback.TtsPlaybackStart
import mihon.tts.api.playback.TtsPlaybackState
import mihon.tts.api.playback.TtsStopReason
import mihon.tts.api.playback.TtsStopResult
import mihon.tts.api.playback.TtsTextRange
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.runtime.audio.TtsAudioFocus
import mihon.tts.spi.engine.TtsEngine
import mihon.tts.spi.engine.TtsEngineExecution
import mihon.tts.spi.engine.TtsEnginePlayback
import mihon.tts.spi.engine.TtsEnginePlaybackEvent
import mihon.tts.spi.engine.TtsEnginePreparation
import java.util.concurrent.atomic.AtomicBoolean

internal class TtsPlaybackCoordinator(
    private val scope: CoroutineScope,
    private val audioFocus: TtsAudioFocus,
    private val mapPreparation: (RuntimeReadyTts, TtsEnginePreparation) -> TtsPreparation,
) {
    private val mutex = Mutex()
    private var current: RuntimeTtsPlaybackSession? = null
    private var retainedEngine: TtsEngine? = null

    suspend fun play(ready: RuntimeReadyTts): TtsPlaybackStart = mutex.withLock {
        val revalidated = ready.engine.revalidate(ready.firstProviderRequest)
        if (revalidated !is TtsEnginePreparation.Ready) {
            return@withLock TtsPlaybackStart.PreparationChanged(mapPreparation(ready, revalidated))
        }

        val replacedSession = current
        val replaceResult = replacedSession?.stopInternal(TtsStopReason.Replaced)
        current = null
        audioFocus.abandon()
        if (replaceResult is TtsStopResult.Failed) {
            return@withLock TtsPlaybackStart.Failed(
                TtsPlaybackFailureReason.ProviderFailure(
                    engine = replacedSession.engine,
                    message = replaceResult.reason,
                ),
            )
        }
        if (retainedEngine !== ready.engine) {
            retainedEngine?.let { engine -> release(engine) }
            retainedEngine = null
        }
        if (!audioFocus.request()) {
            return@withLock TtsPlaybackStart.Failed(TtsPlaybackFailureReason.AudioFocusUnavailable)
        }

        when (val execution = ready.engine.play(revalidated.request)) {
            is TtsEngineExecution.Started -> {
                val session = RuntimeTtsPlaybackSession(
                    ready = ready,
                    firstPlayback = execution.playback,
                    scope = scope,
                    stop = ::stop,
                    finished = ::finished,
                )
                current = session
                retainedEngine = ready.engine
                session.start()
                TtsPlaybackStart.Started(session)
            }

            is TtsEngineExecution.PreparationChanged -> {
                audioFocus.abandon()
                TtsPlaybackStart.PreparationChanged(mapPreparation(ready, execution.preparation))
            }

            is TtsEngineExecution.Failed -> {
                audioFocus.abandon()
                TtsPlaybackStart.Failed(
                    TtsPlaybackFailureReason.ProviderFailure(
                        engine = ready.engine.catalogEntry.id,
                        message = execution.message,
                    ),
                )
            }
        }
    }

    fun onAudioFocusLost() {
        scope.launch {
            mutex.withLock {
                current?.stopInternal(TtsStopReason.AudioFocusLost)
                current = null
                audioFocus.abandon()
            }
        }
    }

    private suspend fun stop(session: RuntimeTtsPlaybackSession): TtsStopResult = mutex.withLock {
        if (current !== session) {
            return@withLock if (session.isTerminal) TtsStopResult.AlreadyTerminal else TtsStopResult.Superseded
        }
        try {
            session.stopInternal(TtsStopReason.Requested)
        } finally {
            current = null
            audioFocus.abandon()
        }
    }

    private fun finished(session: RuntimeTtsPlaybackSession) {
        scope.launch {
            mutex.withLock {
                if (current === session) {
                    current = null
                    audioFocus.abandon()
                }
            }
        }
    }

    private suspend fun release(engine: TtsEngine) {
        try {
            engine.release()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // A failed cleanup must not prevent a different engine from starting.
        }
    }
}

private class RuntimeTtsPlaybackSession(
    private val ready: RuntimeReadyTts,
    private val firstPlayback: TtsEnginePlayback,
    private val scope: CoroutineScope,
    private val stop: suspend (RuntimeTtsPlaybackSession) -> TtsStopResult,
    private val finished: (RuntimeTtsPlaybackSession) -> Unit,
) : TtsPlaybackSession {
    private val mutableState = MutableStateFlow<TtsPlaybackState>(TtsPlaybackState.Starting)
    private val terminal = AtomicBoolean()
    private var playback: TtsEnginePlayback? = firstPlayback
    private var playbackJob: Job? = null

    override val state: StateFlow<TtsPlaybackState> = mutableState
    val engine = ready.engine.catalogEntry.id
    val isTerminal: Boolean
        get() = terminal.get()

    fun start() {
        playbackJob = scope.launch {
            try {
                playSegments()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                fail("TTS playback failed unexpectedly")
            }
        }
    }

    override suspend fun stop(): TtsStopResult = stop(this)

    suspend fun stopInternal(reason: TtsStopReason): TtsStopResult {
        if (!terminal.compareAndSet(false, true)) return TtsStopResult.AlreadyTerminal
        val providerResult = try {
            withContext(NonCancellable) { playback?.stop() }
        } catch (error: Exception) {
            mihon.tts.spi.engine.TtsEngineStopResult.Failed(error.message?.takeUnless(String::isBlank))
        }
        playbackJob?.cancel()
        return when (providerResult) {
            null,
            mihon.tts.spi.engine.TtsEngineStopResult.Stopped,
            mihon.tts.spi.engine.TtsEngineStopResult.AlreadyTerminal,
            -> {
                mutableState.value = TtsPlaybackState.Stopped(reason)
                TtsStopResult.Stopped
            }

            is mihon.tts.spi.engine.TtsEngineStopResult.Failed -> {
                val message = providerResult.message ?: "TTS provider could not stop playback"
                mutableState.value = TtsPlaybackState.Failed(
                    TtsPlaybackFailureReason.ProviderFailure(ready.engine.catalogEntry.id, message),
                )
                TtsStopResult.Failed(message)
            }
        }
    }

    private suspend fun playSegments() {
        var currentPlayback = firstPlayback
        ready.segments.forEachIndexed { index, segment ->
            if (index > 0) {
                val segmentRequest = ready.request.copy(text = segment.text)
                val preparation = ready.engine.prepare(segmentRequest)
                val providerReady = preparation as? TtsEnginePreparation.Ready
                    ?: return fail("TTS provider became unavailable between segments")
                currentPlayback = when (val execution = ready.engine.play(providerReady.request)) {
                    is TtsEngineExecution.Started -> execution.playback
                    is TtsEngineExecution.PreparationChanged ->
                        return fail("TTS provider readiness changed between segments")
                    is TtsEngineExecution.Failed -> return fail(execution.message)
                }
                playback = currentPlayback
            }

            val terminalEvent = currentPlayback.events.first { event ->
                when (event) {
                    TtsEnginePlaybackEvent.Started -> mutableState.value = TtsPlaybackState.Speaking()
                    is TtsEnginePlaybackEvent.RangeStarted -> {
                        mutableState.value = TtsPlaybackState.Speaking(
                            TtsTextRange(
                                startInclusive = segment.startOffset + event.range.startInclusive,
                                endExclusive = segment.startOffset + event.range.endExclusive,
                            ),
                        )
                    }
                    TtsEnginePlaybackEvent.Completed,
                    TtsEnginePlaybackEvent.Stopped,
                    is TtsEnginePlaybackEvent.Failed,
                    -> Unit
                }
                event is TtsEnginePlaybackEvent.Completed ||
                    event is TtsEnginePlaybackEvent.Stopped ||
                    event is TtsEnginePlaybackEvent.Failed
            }
            when (terminalEvent) {
                TtsEnginePlaybackEvent.Completed -> Unit
                TtsEnginePlaybackEvent.Stopped -> return stoppedByProvider()
                is TtsEnginePlaybackEvent.Failed -> return fail(terminalEvent.message)
                else -> error("Non-terminal TTS event escaped terminal selection")
            }
        }
        if (terminal.compareAndSet(false, true)) {
            mutableState.value = TtsPlaybackState.Completed
            finished(this)
        }
    }

    private fun stoppedByProvider() {
        if (terminal.compareAndSet(false, true)) {
            mutableState.value = TtsPlaybackState.Stopped(TtsStopReason.Requested)
            finished(this)
        }
    }

    private fun fail(message: String?) {
        if (terminal.compareAndSet(false, true)) {
            mutableState.value = TtsPlaybackState.Failed(
                TtsPlaybackFailureReason.ProviderFailure(
                    engine = ready.engine.catalogEntry.id,
                    message = message,
                ),
            )
            finished(this)
        }
    }
}
