package app.katari.video.engine.mpv.proxy

import logcat.LogPriority
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import tachiyomi.core.common.util.system.logcat
import java.io.Closeable
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * How long mpv waits for the proxy's answer to a request before it gives up on it (its `network-timeout`, in seconds).
 * FFmpeg's HLS demuxer then skips the segment, as it does one the proxy answers with an error.
 */
internal const val MPV_NETWORK_TIMEOUT_SECONDS = 60

/**
 * The proxy's calls to hosts, cancelled when it closes. A request mpv waits for is made again when the network fails
 * it or the host answers that it can't serve it now, until the time to answer mpv runs out. A segment that fails is
 * lost to mpv: FFmpeg's HLS demuxer skips it, and where the stream's video and an audio track come from separate
 * playlists, each demuxer skips its own stretch, which mpv then plays out of sync. A network that dropped and came
 * back, as one does when the system wakes from sleep, fails a request or two before it works again, and a CDN whose
 * origin is slow answers a segment with a 5xx such as Cloudflare's 522 once; asking again rides both out, as
 * ExoPlayer asks again for a chunk.
 */
internal class UpstreamCalls(private val client: OkHttpClient) : Closeable {
    private val calls = ConcurrentHashMap.newKeySet<Call>()
    private val deadlines: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "mpv-stream-proxy-deadline").apply { isDaemon = true }
    }

    @Volatile
    private var closed = false

    override fun close() {
        closed = true
        calls.forEach(Call::cancel)
        deadlines.shutdownNow()
    }

    /** Runs [use] on [request]'s response, asked once; throws what the call threw. */
    fun <T> execute(request: Request, use: (Response) -> T): T = tracked(client.newCall(request)) { call ->
        use(call.execute())
    }

    /**
     * Runs [use] on the response to [request] for mpv, asking again after network failures and [asksAgainLater]
     * statuses for as long as [mpvWaits] says mpv still waits for it. Once there is no time left to answer mpv,
     * [unanswered] gets the last failure instead. Returns null without calling either once mpv stopped waiting or the
     * proxy closed.
     */
    fun <T : Any> answerMpv(
        request: Request,
        mpvWaits: () -> Boolean,
        unanswered: (UpstreamFailure) -> T,
        use: (Response) -> T,
    ): T? {
        val deadlineNanos = System.nanoTime() + ANSWER_DEADLINE_NANOS
        var retries = 0
        while (true) {
            val call = client.newCall(request)
            val failure = try {
                return tracked(call) { use(executeBefore(it, deadlineNanos)) }
            } catch (e: NoAnswer) {
                e.failure
            }
            if (closed || !mpvWaits()) return null
            retries++
            val delayNanos = minOf(retries * RETRY_DELAY_STEP_NANOS, MAX_RETRY_DELAY_NANOS)
            if (System.nanoTime() + delayNanos >= deadlineNanos) return unanswered(failure)
            logcat(LogPriority.INFO) {
                "mpv stream proxy: asking again in ${TimeUnit.NANOSECONDS.toSeconds(delayNanos)} s: $failure"
            }
            try {
                TimeUnit.NANOSECONDS.sleep(delayNanos)
            } catch (_: InterruptedException) {
                // The proxy closed.
                Thread.currentThread().interrupt()
                return null
            }
            if (closed || !mpvWaits()) return null
        }
    }

    /**
     * [call]'s response, cancelled at [deadlineNanos] if it hasn't come by then: a connection and a read can each
     * take OkHttp's full timeout, which together outlast mpv's. Failures to get it, and responses with a status that
     * [asksAgainLater], are thrown as [NoAnswer], so they aren't confused with what fails while the response is
     * relayed.
     */
    private fun executeBefore(call: Call, deadlineNanos: Long): Response {
        val lock = Any()
        var answered = false
        var timedOut = false
        val timeout = try {
            deadlines.schedule(
                {
                    synchronized(lock) {
                        if (!answered) {
                            timedOut = true
                            call.cancel()
                        }
                    }
                },
                deadlineNanos - System.nanoTime(),
                TimeUnit.NANOSECONDS,
            )
        } catch (_: RejectedExecutionException) {
            throw networkFailure(call, IOException("The stream proxy closed"))
        }
        try {
            val response = try {
                call.execute()
            } catch (e: IOException) {
                throw networkFailure(call, if (synchronized(lock) { timedOut }) noAnswerInTime() else e)
            }
            synchronized(lock) {
                if (timedOut) {
                    response.close()
                    throw networkFailure(call, noAnswerInTime())
                }
                answered = true
            }
            if (response.code.asksAgainLater()) {
                response.close()
                throw NoAnswer(UpstreamFailure.Status(call.request().url.toString(), response.code))
            }
            return response
        } finally {
            timeout.cancel(false)
        }
    }

    private fun noAnswerInTime() = SocketTimeoutException("The host didn't answer in time for mpv")

    private fun networkFailure(call: Call, cause: IOException) =
        NoAnswer(UpstreamFailure.Network(call.request().url.toString(), cause))

    // Statuses a host answers when it can't serve the request now but may in a moment: a timeout, a rate limit, and
    // its own or its origin's failure, which CDNs report as 5xx (Cloudflare's 520–526). 501 says it never will.
    private fun Int.asksAgainLater() = this == REQUEST_TIMEOUT || this == TOO_MANY_REQUESTS ||
        (this in SERVER_ERRORS && this != NOT_IMPLEMENTED)

    private fun <T> tracked(call: Call, block: (Call) -> T): T {
        calls += call
        if (closed) call.cancel()
        try {
            return block(call)
        } finally {
            calls -= call
        }
    }

    private class NoAnswer(val failure: UpstreamFailure) : Exception()

    private companion object {
        // Time left for the answer to reach mpv.
        val ANSWER_DEADLINE_NANOS = TimeUnit.SECONDS.toNanos(MPV_NETWORK_TIMEOUT_SECONDS - 10L)

        // The first retry waits long enough for OkHttp to have given up on an HTTP/2 connection that no longer
        // answers its ping, which it otherwise hands the retry again.
        val RETRY_DELAY_STEP_NANOS = TimeUnit.SECONDS.toNanos(1)
        val MAX_RETRY_DELAY_NANOS = TimeUnit.SECONDS.toNanos(5)

        const val REQUEST_TIMEOUT = 408
        const val TOO_MANY_REQUESTS = 429
        const val NOT_IMPLEMENTED = 501
        val SERVER_ERRORS = 500..599
    }
}
