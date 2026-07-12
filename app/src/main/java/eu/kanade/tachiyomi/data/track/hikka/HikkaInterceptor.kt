package eu.kanade.tachiyomi.data.track.hikka

import eu.kanade.tachiyomi.data.track.hikka.dto.HKAuthTokenInfo
import eu.kanade.tachiyomi.data.track.hikka.dto.HKOAuth
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

class HikkaInterceptor(private val hikka: Hikka) : Interceptor {
    private val json: Json by injectLazy()
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val profileId = hikka.currentProfileId()
        var currentAuth = hikka.loadOAuth(profileId) ?: throw Exception("Hikka: You are not authorized")

        if (currentAuth.isExpired()) {
            val refreshTokenResponse = chain.proceed(HikkaApi.refreshTokenRequest(currentAuth.accessToken))
            if (!refreshTokenResponse.isSuccessful) {
                refreshTokenResponse.close()
                hikka.clearAuth(profileId)
                throw Exception("Hikka: The token is expired")
            } else {
                refreshTokenResponse.close()
            }

            val authTokenInfoResponse = chain.proceed(HikkaApi.authTokenInfo(currentAuth.accessToken))
            if (!authTokenInfoResponse.isSuccessful) {
                authTokenInfoResponse.close()
                throw Exception("Hikka: Auth token info failed")
            }

            val authTokenInfo = json.decodeFromString<HKAuthTokenInfo>(authTokenInfoResponse.body.string())
            currentAuth = HKOAuth(currentAuth.accessToken, authTokenInfo.expiration, authTokenInfo.created)
            hikka.saveOAuth(currentAuth, profileId)
        }

        val authRequest = originalRequest.newBuilder()
            .addHeader("auth", currentAuth.accessToken)
            .addHeader("accept", "application/json")
            .build()

        return chain.proceed(authRequest)
    }
}
