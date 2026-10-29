package mihon.translation.provider.libretranslate.offline

import okhttp3.OkHttpClient
import java.net.Proxy
import java.util.concurrent.TimeUnit

internal object OfflineTranslatorNetwork {
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .build()
}
