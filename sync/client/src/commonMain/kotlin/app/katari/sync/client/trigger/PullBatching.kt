package app.katari.sync.client.trigger

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Announcements of changes on the server, gathered into pulls (D064): a pull starts once announcements have been
 * quiet for [quiet], and at most [longest] after the first of them, so another device sending its library starts a
 * pull every few seconds instead of one per push it makes.
 */
internal class PullBatching(
    private val quiet: Duration = QUIET,
    private val longest: Duration = LONGEST,
    private val time: TimeSource = TimeSource.Monotonic,
) {
    private val heard = Channel<Unit>(Channel.CONFLATED)

    fun hear() {
        heard.trySend(Unit)
    }

    /** Calls [pull] for each gathered batch of announcements, until cancelled. */
    suspend fun pulls(pull: () -> Unit): Nothing {
        while (true) {
            heard.receive()
            val first = time.markNow()
            while (true) {
                val left = longest - first.elapsedNow()
                if (left <= Duration.ZERO) break
                withTimeoutOrNull(minOf(quiet, left)) { heard.receive() } ?: break
            }
            pull()
        }
    }

    private companion object {
        val QUIET = 2.seconds
        val LONGEST = 10.seconds
    }
}
