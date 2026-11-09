package app.katari.sync.client.trigger

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * When a stream of signals, such as edits on this device or the server's announcements of other devices' changes, gets
 * a run: the first signal after a pause at once, so a single change reaches other devices in about the time a run
 * takes, and while signals keep coming at most once every [gap], for all that came since. The gap keeps a stream, such
 * as a library update writing one entry after another or another device sending its library, from running syncs back
 * to back.
 */
internal class RunPacing(
    private val gap: Duration = GAP,
    private val time: TimeSource = TimeSource.Monotonic,
) {
    private val heard = Channel<Unit>(Channel.CONFLATED)

    fun hear() {
        heard.trySend(Unit)
    }

    /** Calls [act] for the signals heard as described above, until cancelled. */
    suspend fun follow(act: suspend () -> Unit): Nothing {
        var last: TimeMark? = null
        while (true) {
            heard.receive()
            last?.let { delay(gap - it.elapsedNow()) }
            // What came during the wait is covered by this act; what comes during it gets the next
            heard.tryReceive()
            last = time.markNow()
            act()
        }
    }

    private companion object {
        val GAP: Duration = 2.seconds
    }
}
