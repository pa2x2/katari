package eu.kanade.tachiyomi.data.track.mangabaka

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

class MangaBakaInterceptor(private val mangaBaka: MangaBaka) : Interceptor {

    private val json: Json by injectLazy()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val profileId = mangaBaka.currentProfileId()
        var currentAuth = mangaBaka.restoreToken(profileId) ?: throw Exception("Not authenticated with MangaBaka")

        if (currentAuth.isExpired()) {
            val response = chain.proceed(MangaBakaApi.refreshTokenRequest(currentAuth.refreshToken))
            if (response.isSuccessful) {
                currentAuth = json.decodeFromString(response.body.string())
                mangaBaka.saveToken(currentAuth, profileId)
            } else {
                response.close()
                throw Exception("Could not refresh MangaBaka authentication")
            }
        }

        return originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer ${currentAuth.accessToken}")
            .build()
            .let(chain::proceed)
    }
}
