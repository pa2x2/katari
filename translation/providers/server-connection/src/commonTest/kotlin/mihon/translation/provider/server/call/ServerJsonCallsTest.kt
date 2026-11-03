package mihon.translation.provider.server.call

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class ServerJsonCallsTest {

    @Test
    fun `server failures do not expose response payloads`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse.Builder()
                    .code(400)
                    .body("""{"error":"selected text must never escape diagnostics"}""")
                    .build(),
            )
            server.start()

            val error = shouldThrow<ServerCallException> { calls().answer<String>(request(server)) }

            error.failure shouldBe ServerCallFailure.Status(400)
            error.message.orEmpty() shouldNotContain "selected text"
        }
    }

    @Test
    fun `cancelling a request cancels the HTTP wait`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse.Builder()
                    .body("\"late\"")
                    .bodyDelay(30, TimeUnit.SECONDS)
                    .build(),
            )
            server.start()
            val answer = async(start = CoroutineStart.UNDISPATCHED) { calls().answer<String>(request(server)) }

            (server.takeRequest(5, TimeUnit.SECONDS) != null) shouldBe true
            answer.cancel()

            shouldThrow<CancellationException> { answer.await() }
        }
    }

    @Test
    fun `a busy server is asked again until it answers but a rejected request is not repeated`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse.Builder().code(429).addHeader("Retry-After", "2").build())
            server.enqueue(MockResponse.Builder().code(503).build())
            server.enqueue(MockResponse.Builder().body("\"answered\"").build())
            server.enqueue(MockResponse.Builder().code(400).build())
            server.start()
            val calls = ServerJsonCalls(OkHttpClient(), retries = 2)

            calls.answer<String>(request(server)) shouldBe "answered"
            server.requestCount shouldBe 3

            shouldThrow<ServerCallException> { calls.answer<String>(request(server)) }
                .failure shouldBe ServerCallFailure.Status(400)
            server.requestCount shouldBe 4
        }
    }

    private fun calls() = ServerJsonCalls(OkHttpClient())

    private fun request(server: MockWebServer) = Request.Builder().url(server.url("/")).build()
}
