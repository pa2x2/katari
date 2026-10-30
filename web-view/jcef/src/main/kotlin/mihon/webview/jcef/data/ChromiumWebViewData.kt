package mihon.webview.jcef.data

import eu.kanade.presentation.more.settings.screen.advanced.WebViewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.core.platform.PlatformContext
import mihon.webview.jcef.runtime.JcefHost
import mihon.webview.jcef.runtime.JcefPresence
import mihon.webview.jcef.runtime.JcefRuntime

/**
 * The web view's data on Chromium, which the app offers to delete as Android's: Chromium's whole profile, its caches,
 * site storage, history and form data. Its cookies stay the app's cookie store's, which "Clear cookies" clears.
 */
class ChromiumWebViewData(
    private val runtime: JcefRuntime,
    host: JcefHost,
    private val origins: SiteOrigins,
) : WebViewData {
    private val profile by lazy { ChromiumProfileData(host.dataDirectory) }

    override val isOffered: Boolean = true

    // Chromium's caches go with the rest of its profile, which deleting needs Chromium's answer for, so it is done with
    // the storage
    override fun clearCaches(context: PlatformContext) = Unit

    override suspend fun deleteStorage(context: PlatformContext) = withContext(Dispatchers.IO) {
        if (runtime.hasStarted) {
            // What it holds in memory goes now, and its files once it starts again
            LoadedSiteData.clear(runtime.start(), origins.all())
            profile.deleteAtNextStart()
        } else {
            profile.delete()
        }
    }

    override suspend fun version(context: PlatformContext): String =
        (runtime.presence as? JcefPresence.Present)?.version ?: error("This Java runtime has no JCEF")
}
