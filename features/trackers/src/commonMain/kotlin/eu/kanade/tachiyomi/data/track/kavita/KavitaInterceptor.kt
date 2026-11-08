package eu.kanade.tachiyomi.data.track.kavita

import kotlinx.coroutines.runBlocking
import mihon.domain.app.AppIdentity
import okhttp3.Interceptor
import okhttp3.Response

class KavitaInterceptor(private val kavita: Kavita, private val appIdentity: AppIdentity) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (kavita.authentications == null) {
            runBlocking { kavita.loadOAuth() }
        }
        val jwtToken = kavita.authentications?.getToken(
            kavita.api.getApiFromUrl(originalRequest.url.toString()),
        )

        // Add the authorization header to the original request.
        val authRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $jwtToken")
            .header("User-Agent", "Katari v${appIdentity.versionName} (${appIdentity.applicationId})")
            .build()

        return chain.proceed(authRequest)
    }
}
