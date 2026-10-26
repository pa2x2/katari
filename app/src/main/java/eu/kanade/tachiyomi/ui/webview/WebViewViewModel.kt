package eu.kanade.tachiyomi.ui.webview

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import logcat.LogPriority
import mihon.app.di.AppUiScope
import mihon.entry.interactions.source.EntryWebViewFeature
import mihon.entry.interactions.source.EntryWebViewHeadersResolution
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tachiyomi.core.common.util.system.logcat

@AssistedInject
class WebViewViewModel(
    @Assisted val sourceId: Long?,
    private val webViewFeature: EntryWebViewFeature,
    private val network: NetworkHelper,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(sourceId: Long?): WebViewViewModel
    }

    val state: StateFlow<StatsScreenState>
        field = MutableStateFlow<StatsScreenState>(StatsScreenState.Loading)

    var headers = emptyMap<String, String>()

    val siteStorageSource = sourceId?.let(webViewFeature::siteStorageSource)

    init {
        when (val resolution = sourceId?.let(webViewFeature::resolveHeaders)) {
            is EntryWebViewHeadersResolution.Available -> headers = resolution.headers
            is EntryWebViewHeadersResolution.Failed -> {
                logcat(LogPriority.ERROR, resolution.cause) { "Failed to build headers" }
            }
            else -> Unit
        }
    }

    fun shareWebpage(context: Context, url: String) {
        try {
            context.startActivity(url.toUri().toShareIntent(context, type = "text/plain"))
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    fun openInBrowser(context: Context, url: String) {
        context.openInBrowser(url, forceDefaultBrowser = true)
    }

    fun clearCookies(url: String) {
        url.toHttpUrlOrNull()?.let {
            val cleared = network.cookieJar.remove(it)
            logcat { "Cleared $cleared cookies for: $url" }
        }
    }

    fun defaultUserAgentProvider() = network.defaultUserAgentProvider()
}
