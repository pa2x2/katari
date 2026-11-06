package app.katari.sync.client

import app.katari.sync.protocol.http.SyncHttp
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/** Cuts a device's calls short as a dropped connection would, and counts the pushes that reached the server. */
internal class CallCutter : Interceptor {
    /** Pulls left before the next one fails without reaching the server; null lets every pull through. */
    @Volatile
    var pullsBeforeCut: Int? = null

    /** The next push reaches the server, but its answer never comes back. */
    @Volatile
    var loseNextPushAnswer = false

    val pushes = AtomicInteger()

    val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(this).build()

    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath
        if (path.endsWith(SyncHttp.PULL)) {
            pullsBeforeCut?.let { left ->
                pullsBeforeCut = if (left == 0) null else left - 1
                if (left == 0) throw IOException("Connection dropped")
            }
        }
        val response = chain.proceed(chain.request())
        if (path.endsWith(SyncHttp.PUSH)) {
            pushes.incrementAndGet()
            if (loseNextPushAnswer) {
                loseNextPushAnswer = false
                response.close()
                throw IOException("Connection dropped before the answer")
            }
        }
        return response
    }
}
