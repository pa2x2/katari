package mihon.model.artifacts.runtime.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Client for large artifact transfers: bounded connection setup and stalls, but no whole-call deadline. */
internal object ModelArtifactNetwork {
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
}
