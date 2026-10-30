package mihon.desktop.webview

import eu.kanade.presentation.more.settings.screen.advanced.WebViewData
import mihon.core.platform.PlatformContext

/** Without JCEF in the Java runtime the app runs on, desktop has no web view, so it keeps no web view data. */
class NoWebViewData : WebViewData {
    override val isOffered: Boolean = false

    override fun clearCaches(context: PlatformContext) = Unit

    override suspend fun deleteStorage(context: PlatformContext) = Unit

    override suspend fun version(context: PlatformContext): String = error(
        "The web view needs a Java runtime with JCEF",
    )
}
