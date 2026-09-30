package mihon.translation.provider.deepl

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal object DeepLServerNetwork {
    /** A whole page is translated per call, which takes a language model on a server of the user's own a while. */
    val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(150, TimeUnit.SECONDS)
        .build()
}
