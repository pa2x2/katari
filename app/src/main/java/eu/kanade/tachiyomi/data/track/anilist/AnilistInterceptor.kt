package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.track.anilist.dto.ALOAuth
import eu.kanade.tachiyomi.data.track.anilist.dto.isExpired
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AnilistInterceptor(val anilist: Anilist) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Anilist returns the date without milliseconds. Expire one minute early.
        val oauth = anilist.loadOAuth()
            ?.let { it.copy(expires = it.expires * 1000 - 60 * 1000) }
            ?: throw Exception("Not authenticated with Anilist")
        if (oauth.isExpired()) {
            anilist.logout()
            throw IOException("Token expired")
        }

        // Add the authorization header to the original request.
        val authRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer ${oauth.accessToken}")
            .header("User-Agent", "Katari v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})")
            .build()

        return chain.proceed(authRequest)
    }

    /**
     * Called when the user authenticates with Anilist for the first time. Sets the refresh token
     * and the oauth object.
     */
    fun setAuth(oauth: ALOAuth?) {
        anilist.saveOAuth(oauth)
    }
}
