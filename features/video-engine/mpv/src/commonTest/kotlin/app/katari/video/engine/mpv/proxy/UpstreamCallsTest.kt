package app.katari.video.engine.mpv.proxy

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class UpstreamCallsTest {

    private val request = Request.Builder().url("https://host.example/segment-220.ts").build()

    /**
     * A host that fails the first [failures] attempts, with [failureStatus] or through the network when it's null, and
     * then answers with a segment.
     */
    private fun upstream(failures: Int, attempts: AtomicInteger, failureStatus: Int? = null) = UpstreamCalls(
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val failed = attempts.incrementAndGet() <= failures
                if (failed && failureStatus == null) throw IOException("timeout")
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(if (failed) failureStatus!! else 200)
                    .message(if (failed) "Error" else "OK")
                    .body((if (failed) "" else "segment").toResponseBody())
                    .build()
            }
            .build(),
    )

    @Test
    fun `a request the network failed is answered once the network works again`() {
        val attempts = AtomicInteger()
        val calls = upstream(failures = 1, attempts)

        val body = calls.answerMpv(request, mpvWaits = { true }, unanswered = { "unanswered" }) { response ->
            response.use { it.body.string() }
        }

        body shouldBe "segment"
        attempts.get() shouldBe 2
        calls.close()
    }

    // Cloudflare answers 522 when the origin is slow to answer it: passed on, it made mpv skip a separate audio
    // track's segment and play the audio seconds ahead of the video.
    @Test
    fun `a segment the host can't serve for now is asked for again`() {
        val attempts = AtomicInteger()
        val calls = upstream(failures = 1, attempts, failureStatus = 522)

        val body = calls.answerMpv(request, mpvWaits = { true }, unanswered = { "unanswered" }) { response ->
            response.use { it.body.string() }
        }

        body shouldBe "segment"
        attempts.get() shouldBe 2
        calls.close()
    }

    // mpv closes its connection once it stops, as the player stops it to load the stream again after a failure; a
    // failure reported then would fail the new load.
    @Test
    fun `a request mpv stopped waiting for is neither made again nor reported`() {
        val attempts = AtomicInteger()
        val calls = upstream(failures = Int.MAX_VALUE, attempts)

        val answer = calls.answerMpv(request, mpvWaits = { false }, unanswered = { "unanswered" }) { "answered" }

        answer.shouldBeNull()
        attempts.get() shouldBe 1
        calls.close()
    }
}
