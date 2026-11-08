package eu.kanade.tachiyomi.ui.webview

import androidx.lifecycle.ViewModel
import app.katari.core.platform.PlatformContext
import app.katari.core.platform.link.LinkOpener
import app.katari.core.platform.share.LinkSharing
import app.katari.entry.interactions.source.EntryWebViewFeature
import app.katari.entry.interactions.source.EntryWebViewHeadersResolution
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.tachiyomi.network.NetworkHelper
import logcat.LogPriority
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tachiyomi.core.common.util.system.logcat

/** What the web view screen asks of the app: [sourceId]'s headers and site storage, and the page's menu actions. */
@AssistedInject
class WebViewViewModel(
    @Assisted sourceId: Long?,
    webViewFeature: EntryWebViewFeature,
    private val network: NetworkHelper,
    private val linkOpener: LinkOpener,
    private val linkSharing: LinkSharing,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(sourceId: Long?): WebViewViewModel
    }

    val headers: Map<String, String> = when (val resolution = sourceId?.let(webViewFeature::resolveHeaders)) {
        is EntryWebViewHeadersResolution.Available -> resolution.headers
        is EntryWebViewHeadersResolution.Failed -> {
            logcat(LogPriority.ERROR, resolution.cause) { "Failed to build headers" }
            emptyMap()
        }
        else -> emptyMap()
    }

    val siteStorageSource = sourceId?.let(webViewFeature::siteStorageSource)

    val canShare: Boolean get() = linkSharing.isAvailable

    fun shareWebpage(context: PlatformContext, url: String) = linkSharing.share(context, url)

    fun openInBrowser(context: PlatformContext, url: String) = linkOpener.open(context, url, forceDefaultBrowser = true)

    fun clearCookies(url: String) {
        url.toHttpUrlOrNull()?.let {
            val cleared = network.cookieJar.remove(it)
            logcat { "Cleared $cleared cookies for: $url" }
        }
    }

    fun defaultUserAgentProvider() = network.defaultUserAgentProvider()
}
