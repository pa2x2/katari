package eu.kanade.tachiyomi.data.track.kitsu

import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuOAuth
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import mihon.domain.app.AppIdentity
import okhttp3.Interceptor
import okhttp3.Response

class KitsuInterceptor(
    private val kitsu: Kitsu,
    private val json: Json,
    private val appIdentity: AppIdentity,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        var currentAuth = kitsu.restoreToken() ?: throw Exception("Not authenticated with Kitsu")

        val refreshToken = currentAuth.refreshToken!!

        // Refresh access token if expired.
        if (currentAuth.isExpired()) {
            val response = chain.proceed(KitsuApi.refreshTokenRequest(refreshToken))
            if (response.isSuccessful) {
                currentAuth = with(json) {
                    response.parseAs<KitsuOAuth>()
                }
                newAuth(currentAuth)
            } else {
                response.close()
            }
        }

        // Add the authorization header to the original request.
        val authRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer ${currentAuth.accessToken}")
            .header("User-Agent", "Katari v${appIdentity.versionName} (${appIdentity.applicationId})")
            .header("Accept", "application/vnd.api+json")
            .header("Content-Type", "application/vnd.api+json")
            .build()

        return chain.proceed(authRequest)
    }

    fun newAuth(oauth: KitsuOAuth?) {
        kitsu.saveToken(oauth)
    }
}
