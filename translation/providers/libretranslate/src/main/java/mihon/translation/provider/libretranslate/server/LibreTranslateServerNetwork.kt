package mihon.translation.provider.libretranslate.server

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal object LibreTranslateServerNetwork {
    val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .build()
}
