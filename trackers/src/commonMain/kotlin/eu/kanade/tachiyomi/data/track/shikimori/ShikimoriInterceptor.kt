package eu.kanade.tachiyomi.data.track.shikimori

import eu.kanade.tachiyomi.data.track.shikimori.dto.SMOAuth
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import mihon.domain.app.AppIdentity
import okhttp3.Interceptor
import okhttp3.Response

class ShikimoriInterceptor(
    private val shikimori: Shikimori,
    private val json: Json,
    private val appIdentity: AppIdentity,
) : Interceptor {

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
            .header("User-Agent", "Katari v${appIdentity.versionName} (${appIdentity.applicationId})")
            .build()

        return chain.proceed(authRequest)
    }

    fun newAuth(oauth: SMOAuth?) {
        shikimori.saveToken(oauth)
    }
}
