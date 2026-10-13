package eu.kanade.tachiyomi.data.track.shikimori

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMOAuth
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

class ShikimoriInterceptor(private val shikimori: Shikimori) : Interceptor {

    private val json: Json by injectLazy()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        var currentAuth = shikimori.restoreToken() ?: throw Exception("Not authenticated with Shikimori")

        // Refresh access token if expired.
        if (currentAuth.isExpired()) {
            val response = chain.proceed(ShikimoriApi.refreshTokenRequest(currentAuth.refreshToken!!))
            if (response.isSuccessful) {
                currentAuth = with(json) {
                    response.parseAs<SMOAuth>()
                }
                newAuth(currentAuth)
            } else {
                response.close()
            }
        }
        // Add the authorization header to the original request.
        val authRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer ${currentAuth.accessToken}")
            .header("User-Agent", "Katari v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})")
            .build()

        return chain.proceed(authRequest)
    }

    fun newAuth(oauth: SMOAuth?) {
        shikimori.saveToken(oauth)
    }
}
