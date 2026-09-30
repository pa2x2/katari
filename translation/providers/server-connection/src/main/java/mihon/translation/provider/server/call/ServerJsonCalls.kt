package mihon.translation.provider.server.call

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

/** Asks a translation server questions it answers in JSON, failing with a [ServerCallException] when it does not. */
class ServerJsonCalls(
    private val httpClient: OkHttpClient,
) {
    /** What the server answers to [request]. */
    suspend inline fun <reified T> answer(request: Request): T = answer(request, serializer<T>())

    suspend fun <T> answer(request: Request, answer: DeserializationStrategy<T>): T {
        val body = execute(request)
        return try {
            JSON.decodeFromString(answer, body)
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

    /**
     * Keeps the call cancellable until its body has been read, so cancelling a translation aborts
     * a slow response body instead of waiting for the socket read timeout.
     */
    private suspend fun execute(request: Request): String {
        val call = httpClient.newCall(request)
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
                        continuation.resumeWith(runCatching { response.use(::readBody) })
                    }
                },
            )
        }
    }

    private fun readBody(response: Response): String {
        if (!response.isSuccessful) throw ServerCallException(ServerCallFailure.Status(response.code))
        return try {
            response.body.string()
        } catch (e: IOException) {
            throw ServerCallException(ServerCallFailure.Connection, e)
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
