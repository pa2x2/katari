package app.katari.sync.client

import app.katari.sync.protocol.http.SyncHttp
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/** Cuts a device's calls short as a dropped connection would, and counts the pushes that reached the server. */
internal class CallCutter : Interceptor {
    /** Pulls left before the next one fails without reaching the server; null lets every pull through. */
    @Volatile
    var pullsBeforeCut: Int? = null

    /** The next push reaches the server, but its answer never comes back. */
    @Volatile
    var loseNextPushAnswer = false

    /** The next file upload stops halfway through its bytes, as a connection dropped mid-upload leaves it. */
    @Volatile
    var cutNextUpload = false

    val pushes = AtomicInteger()

    @Volatile
    private var held: Pair<String, CountDownLatch>? = null

    /**
     * The next call to [path] waits, as on a server that took the connection and hasn't answered, until the returned
     * latch is counted down.
     */
    fun holdNext(path: String): CountDownLatch = CountDownLatch(1).also { held = path to it }

    val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(this).build()

    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath
        held?.takeIf { path.endsWith(it.first) }?.let { (_, latch) ->
            held = null
            latch.await()
        }
        val body = chain.request().body
        if (cutNextUpload && chain.request().method == "PUT" && body != null) {
            cutNextUpload = false
            return chain.proceed(chain.request().newBuilder().put(HalfBody(body)).build())
        }
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

private class HalfBody(private val whole: RequestBody) : RequestBody() {
    override fun contentType(): MediaType? = whole.contentType()

    override fun contentLength(): Long = whole.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val bytes = Buffer().also(whole::writeTo)
        sink.write(bytes, bytes.size / 2)
        sink.flush()
        throw IOException("Connection dropped mid-upload")
    }
}
