package eu.kanade.tachiyomi.data.track.bangumi

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMOAuth
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response

class BangumiInterceptor(private val bangumi: Bangumi, private val json: Json) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        var currAuth: BGMOAuth = bangumi.restoreToken() ?: throw Exception("Not authenticated with Bangumi")

        if (currAuth.isExpired()) {
            val response = chain.proceed(BangumiApi.refreshTokenRequest(currAuth.refreshToken!!))
            if (response.isSuccessful) {
                currAuth = with(json) {
                    response.parseAs<BGMOAuth>()
                }
                newAuth(currAuth)
            } else {
                response.close()
            }
        }

        return originalRequest.newBuilder()
            .header(
                "User-Agent",
                "pa2x2/Katari/v${BuildConfig.VERSION_NAME} (Android) (https://github.com/pa2x2/katari)",
            )
            .addHeader("Authorization", "Bearer ${currAuth.accessToken}")
            .build()
            .let(chain::proceed)
    }

    fun newAuth(oauth: BGMOAuth?) {
        bangumi.saveToken(oauth)
    }
}
