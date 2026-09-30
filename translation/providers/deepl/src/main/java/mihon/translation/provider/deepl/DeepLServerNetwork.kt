package mihon.translation.provider.deepl

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal object DeepLServerNetwork {
    /**
     * A language model on a server of the user's own takes a while before it starts to answer a translation, hence
     * the long read timeout. How long a call may take as a whole depends on what is asked, so that is limited per
     * call instead of here.
     */
    val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}
