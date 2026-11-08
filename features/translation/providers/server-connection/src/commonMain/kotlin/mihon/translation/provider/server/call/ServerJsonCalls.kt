package mihon.translation.provider.server.call

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Asks a translation server questions it answers in JSON, failing with a [ServerCallException] when it does not.
 *
 * A server that answers it is busy, limits how often it is asked, or fails on its end may answer a moment later, so
 * it is asked up to [retries] more times, each after a longer wait.
 */
class ServerJsonCalls(
    private val httpClient: OkHttpClient,
    private val retries: Int = 0,
) {
    /**
     * What the server answers to [request]. Each time it is asked, it has [within] to answer in full, or as long as
     * the client allows when no time is given.
     */
    suspend inline fun <reified T> answer(request: Request, within: Duration? = null): T =
        answer(request, serializer<T>(), within)

    suspend fun <T> answer(request: Request, answer: DeserializationStrategy<T>, within: Duration? = null): T {
        val text = answerText(request, within)
        return try {
            JSON.decodeFromString(answer, text)
        } catch (_: SerializationException) {
            throw ServerCallException(ServerCallFailure.InvalidAnswer)
        } catch (_: IllegalArgumentException) {
            throw ServerCallException(ServerCallFailure.InvalidAnswer)
        }
    }

    /** [payload] as the body of a request. */
    inline fun <reified T> body(payload: T): RequestBody = body(payload, serializer<T>())

    fun <T> body(payload: T, serializer: SerializationStrategy<T>): RequestBody =
        JSON.encodeToString(serializer, payload).toRequestBody(JSON_MEDIA_TYPE)

    private suspend fun answerText(request: Request, within: Duration?): String {
        var retried = 0
        while (true) {
            when (val answer = execute(request, within)) {
                is Answer.Body -> return answer.text
                is Answer.Refusal -> {
                    if (!answer.mayPass || retried == retries) {
                        throw ServerCallException(ServerCallFailure.Status(answer.status))
                    }
                    delay(answer.retryAfter ?: retryDelay(retried))
                    retried++
                }
            }
        }
    }

    /**
     * Keeps the call cancellable until its body has been read, so cancelling a translation aborts
     * a slow response body instead of waiting for the socket read timeout.
     */
    private suspend fun execute(request: Request, within: Duration?): Answer {
        val call = httpClient.newCall(request)
        within?.let { call.timeout().timeout(it.inWholeMilliseconds, TimeUnit.MILLISECONDS) }
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWith(
                            Result.failure(ServerCallException(ServerCallFailure.Connection, e)),
                        )
                    }

                    override fun onResponse(call: Call, response: Response) {
                        continuation.resumeWith(runCatching { response.use(::read) })
                    }
                },
            )
        }
    }

    private fun read(response: Response): Answer {
        if (!response.isSuccessful) {
            val retryAfter = response.header("Retry-After")?.toLongOrNull()?.takeIf { it >= 0 }?.seconds
            return Answer.Refusal(response.code, retryAfter?.coerceAtMost(LONGEST_RETRY_DELAY))
        }
        return try {
            Answer.Body(response.body.string())
        } catch (e: IOException) {
            throw ServerCallException(ServerCallFailure.Connection, e)
        }
    }

    /** Doubles with every retry, and varies so that calls refused together do not all come back together. */
    private fun retryDelay(retried: Int): Duration =
        FIRST_RETRY_DELAY * (1 shl retried) * Random.nextDouble(1.0, 1.0 + RETRY_DELAY_SPREAD)

    private sealed interface Answer {
        data class Body(val text: String) : Answer

        /** The server answered with [status] instead, and asked to wait [retryAfter] when it named a time. */
        data class Refusal(val status: Int, val retryAfter: Duration?) : Answer {
            /** Whether the same request may be answered later: the server is busy, limiting requests, or failing. */
            val mayPass: Boolean
                get() = status == TOO_MANY_REQUESTS || status in 500..599
        }
    }

    private companion object {
        const val TOO_MANY_REQUESTS = 429
        val FIRST_RETRY_DELAY = 1.seconds
        val LONGEST_RETRY_DELAY = 30.seconds
        const val RETRY_DELAY_SPREAD = 0.25
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
