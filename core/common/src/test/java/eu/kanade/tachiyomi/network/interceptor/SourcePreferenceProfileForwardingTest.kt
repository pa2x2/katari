package eu.kanade.tachiyomi.network.interceptor

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.SourcePreferenceProfile
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class SourcePreferenceProfileForwardingTest {

    // An extension interceptor reading its login on OkHttp's thread would get the active profile's during another's update
    @Test
    fun `an enqueued call's interceptors run in the profile of the work that started it`() {
        val seen = CompletableFuture<Long?>()
        val extensionClient = OkHttpClient.Builder()
            .eventListener(SourcePreferenceProfileForwarding)
            .addInterceptor(SourcePreferenceProfileForwarding)
            .build()
            .newBuilder()
            .addInterceptor { chain ->
                seen.complete(SourcePreferenceProfile.current)
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("".toResponseBody())
                    .build()
            }
            .build()

        runBlocking {
            withContext(SourcePreferenceProfile.of(7L)) {
                extensionClient.newCall(Request.Builder().url("https://example.org/").build()).enqueue(
                    object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            seen.completeExceptionally(e)
                        }

                        override fun onResponse(call: Call, response: Response) = response.close()
                    },
                )
            }
        }

        assertEquals(7L, seen.get(10, TimeUnit.SECONDS))
    }
}
