package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Interceptor
import okhttp3.Response
import tachiyomi.core.common.preference.SourcePreferenceProfile

/**
 * Carries [SourcePreferenceProfile] from the thread that starts a call to the one that runs it. An enqueued call
 * (`await()`, `enqueue`) runs on OkHttp's dispatcher threads, where an extension's interceptor reading its login would
 * otherwise get the active profile's during another profile's library update or download.
 *
 * As the client's event listener it tags the call in `callStart`, which runs on the starting thread; as an application
 * interceptor, ahead of any an extension adds, it sets the profile around the rest of the chain. Clients extensions
 * build with `newBuilder()` keep both, unless they set an event listener of their own.
 */
object SourcePreferenceProfileForwarding : EventListener(), Interceptor {

    override fun callStart(call: Call) {
        val profileId = SourcePreferenceProfile.current ?: return
        call.tag(CallProfile::class) { CallProfile(profileId) }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val profile = chain.call().tag(CallProfile::class) ?: return chain.proceed(chain.request())
        return SourcePreferenceProfile.runIn(profile.id) { chain.proceed(chain.request()) }
    }

    private class CallProfile(val id: Long)
}
