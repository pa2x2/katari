package mihon.desktop.advanced

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.advanced.WebViewData
import mihon.core.platform.PlatformContext

/** Desktop has no web view yet, so it keeps no web view data. Stands in until desktop hosts a web view. */
@Inject
class NoWebViewData : WebViewData {
    override val isOffered: Boolean = false

    override fun clearCaches(context: PlatformContext) = Unit

    override suspend fun deleteStorage(context: PlatformContext) = Unit

    override suspend fun version(context: PlatformContext): String = error("Desktop has no web view")
}
