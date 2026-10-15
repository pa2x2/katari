package eu.kanade.tachiyomi.data.track.comick

import eu.kanade.tachiyomi.data.track.comick.dto.ComickOAuth
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response

class ComickInterceptor(private val comick: Comick, private val json: Json) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val profileId = comick.currentProfileId()
        var currentAuth = comick.restoreToken(profileId) ?: throw Exception("Not authenticated with Comick")

        if (currentAuth.isExpired()) {
            currentAuth = refreshToken(chain, profileId)
        }

        return originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer ${currentAuth.accessToken}")
            .build()
            .let(chain::proceed)
    }

    // Concurrent requests must not each spend the same refresh token.
    private fun refreshToken(chain: Interceptor.Chain, profileId: Long): ComickOAuth = synchronized(this) {
        val latest = comick.restoreToken(profileId) ?: throw Exception("Not authenticated with Comick")
        if (!latest.isExpired()) return@synchronized latest

        val response = chain.proceed(ComickApi.refreshTokenRequest(latest.refreshToken))
        if (response.isSuccessful) {
            with(json) { response.parseAs<ComickOAuth>() }
                .also { comick.saveToken(it, profileId) }
        } else {
            response.close()
            latest
        }
    }
}
