package eu.kanade.tachiyomi.data.track.bangumi

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMOAuth
import eu.kanade.tachiyomi.data.track.bangumi.dto.isExpired
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

class BangumiInterceptor(private val bangumi: Bangumi) : Interceptor {

    private val json: Json by injectLazy()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        var currAuth: BGMOAuth = bangumi.restoreToken() ?: throw Exception("Not authenticated with Bangumi")

        if (currAuth.isExpired()) {
            val response = chain.proceed(BangumiApi.refreshTokenRequest(currAuth.refreshToken!!))
            if (response.isSuccessful) {
                currAuth = json.decodeFromString<BGMOAuth>(response.body.string())
                newAuth(currAuth)
            } else {
                response.close()
            }
        }

        return originalRequest.newBuilder()
            .header(
                "User-Agent",
                "katariapp/Katari/v${BuildConfig.VERSION_NAME} (Android) (https://github.com/katariapp/katari)",
            )
            .apply {
                addHeader("Authorization", "Bearer ${currAuth.accessToken}")
            }
            .build()
            .let(chain::proceed)
    }

    fun newAuth(oauth: BGMOAuth?) {
        val normalizedOAuth = if (oauth == null) {
            null
        } else {
            BGMOAuth(
                oauth.accessToken,
                oauth.tokenType,
                System.currentTimeMillis() / 1000,
                oauth.expiresIn,
                oauth.refreshToken,
                bangumi.restoreToken()?.userId,
            )
        }

        bangumi.saveToken(normalizedOAuth)
    }
}
