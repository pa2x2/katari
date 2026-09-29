package mihon.model.artifacts.runtime.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Adapts the app's client, whose DNS-over-HTTPS setting and connections artifact downloads then share, to large
 * transfers: bounded connection setup and stalls, but no whole-call deadline and no response cache.
 */
internal fun OkHttpClient.forModelArtifacts(): OkHttpClient = newBuilder()
    .cache(null)
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .callTimeout(0, TimeUnit.SECONDS)
    .build()
