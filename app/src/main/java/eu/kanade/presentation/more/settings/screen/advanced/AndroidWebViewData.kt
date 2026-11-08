package eu.kanade.presentation.more.settings.screen.advanced

import android.webkit.WebStorage
import android.webkit.WebView
import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.WebViewUtil
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Inject
class AndroidWebViewData : WebViewData {
    override val isOffered: Boolean = true

    override fun clearCaches(context: PlatformContext) {
        WebView(context).run {
            setDefaultSettings()
            clearCache(true)
            clearFormData()
            clearHistory()
            clearSslPreferences()
        }
        WebStorage.getInstance().deleteAllData()
    }

    override suspend fun deleteStorage(context: PlatformContext) {
        context.applicationInfo?.dataDir?.let { File("$it/app_webview/").deleteRecursively() }
    }

    override suspend fun version(context: PlatformContext): String =
        // Loads the WebView package's label through the package manager
        withContext(Dispatchers.IO) { WebViewUtil.getVersion(context) }
}
