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

    /** A host the network fails for the first [failures] attempts, which then answers with a segment. */
    private fun upstream(failures: Int, attempts: AtomicInteger) = UpstreamCalls(
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                if (attempts.incrementAndGet() <= failures) throw IOException("timeout")
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("segment".toResponseBody())
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
