package app.katari.android.di

import android.content.Context
import app.katari.core.metro.AppCoroutineScope
import app.katari.profiles.core.ProfileStore
import app.katari.webpages.host.AndroidHostPages
import app.katari.webpages.keeper.WebPageKeeper
import app.katari.webpages.keeper.WebPageLimits
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.interceptor.WebViewInterceptorMessages
import eu.kanade.tachiyomi.source.entry.webpage.WebPages
import eu.kanade.tachiyomi.ui.webview.browser.AndroidBrowserView
import eu.kanade.tachiyomi.ui.webview.browser.BrowserView
import eu.kanade.tachiyomi.ui.webview.setup.SystemWebViewSetup
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetup
import kotlinx.coroutines.CoroutineScope

@BindingContainer
interface WebViewBindings {
    @Binds
    val AndroidBrowserView.browserView: BrowserView

    @Binds
    val SystemWebViewSetup.webViewSetup: WebViewSetup

    companion object {
        // A phone's memory holds three pages of the weight of a browser tab; all of them share one renderer process
        @Provides
        @SingleIn(AppScope::class)
        fun webPages(
            context: Context,
            network: Lazy<NetworkHelper>,
            messages: WebViewInterceptorMessages,
            profiles: ProfileStore,
            @AppCoroutineScope scope: CoroutineScope,
        ): WebPages = WebPageKeeper(
            hostPages = AndroidHostPages(context, { network.value.defaultUserAgentProvider() }, messages),
            limits = WebPageLimits(perSource = 2, total = 3),
            activeProfileId = { profiles.currentProfileId },
            scope = scope,
        )
    }
}
