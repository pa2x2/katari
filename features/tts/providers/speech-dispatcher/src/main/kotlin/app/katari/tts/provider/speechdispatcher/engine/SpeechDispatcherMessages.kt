package app.katari.tts.provider.speechdispatcher.engine

import app.katari.tts.provider.speechdispatcher.protocol.SsipEvent
import app.katari.tts.provider.speechdispatcher.protocol.SsipEventKind

/**
 * Routes Speech Dispatcher's events to the playback of the message they are about. A message's first event can arrive
 * before the reply that names its id reaches the caller, so events for ids newer than any playback yet are held
 * until that playback is added. Message ids only grow within one server, so older unknown ids are messages already
 * finished with.
 */
internal class SpeechDispatcherMessages {
    private val playbacks = mutableMapOf<Long, SpeechDispatcherPlayback>()
    private val heldEvents = mutableMapOf<Long, MutableList<SsipEventKind>>()
    private var newestMessage = 0L

    @Synchronized
    fun add(messageId: Long, playback: SpeechDispatcherPlayback) {
        newestMessage = maxOf(newestMessage, messageId)
        heldEvents.keys.removeAll { it < messageId }
        playbacks[messageId] = playback
        heldEvents.remove(messageId)?.forEach { deliver(messageId, playback, it) }
    }

    @Synchronized
    fun remove(playback: SpeechDispatcherPlayback) {
        playbacks.values.remove(playback)
    }

    @Synchronized
    fun receive(event: SsipEvent) {
        val playback = playbacks[event.messageId]
        when {
            playback != null -> deliver(event.messageId, playback, event.kind)
            event.messageId > newestMessage -> heldEvents.getOrPut(event.messageId, ::mutableListOf) += event.kind
        }
    }

    /** Fails every playback, for a server that went away, whose next one numbers its messages afresh. */
    @Synchronized
    fun failAll(message: String) {
        playbacks.values.forEach { it.failed(message) }
        playbacks.clear()
        heldEvents.clear()
        newestMessage = 0L
    }

    private fun deliver(messageId: Long, playback: SpeechDispatcherPlayback, kind: SsipEventKind) {
        when (kind) {
            SsipEventKind.Begin -> playback.started()
            SsipEventKind.End -> {
                playbacks.remove(messageId)
                playback.completed()
            }
            SsipEventKind.Canceled -> {
                playbacks.remove(messageId)
                playback.stopped()
            }
            SsipEventKind.IndexMark,
            SsipEventKind.Paused,
            SsipEventKind.Resumed,
            -> Unit
        }
    }
}
