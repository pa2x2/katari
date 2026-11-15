package app.katari.desktop.di

import app.katari.core.common.network.cookie.AppCookieStore
import app.katari.core.common.web.ChallengePage
import app.katari.core.common.web.ExtensionPages
import app.katari.core.metro.AppCoroutineScope
import app.katari.core.platform.clipboard.TextClipboard
import app.katari.core.platform.link.LinkOpener
import app.katari.desktop.webview.DesktopJcefHost
import app.katari.desktop.webview.WindowWebViewDestination
import app.katari.profiles.core.ProfileStore
import app.katari.webpages.host.DesktopHostPages
import app.katari.webpages.keeper.WebPageKeeper
import app.katari.webpages.keeper.WebPageLimits
import app.katari.webview.engine.cookie.ChromiumCookieMirror
import app.katari.webview.engine.data.SiteOrigins
import app.katari.webview.jcef.data.ChromiumWebViewData
import app.katari.webview.jcef.page.challenge.ChromiumChallengePage
import app.katari.webview.jcef.page.extension.ChromiumExtensionPages
import app.katari.webview.jcef.runtime.JbrJcefRuntime
import app.katari.webview.jcef.runtime.JcefRuntime
import app.katari.webview.jcef.surface.ChromiumBrowserView
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.more.settings.screen.advanced.WebViewData
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.interceptor.WebViewInterceptorMessages
import eu.kanade.tachiyomi.source.entry.webpage.WebPages
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import eu.kanade.tachiyomi.ui.webview.browser.BrowserView
import kotlinx.coroutines.CoroutineScope

/**
 * The web view, the pages challenges are solved in and the pages extensions' own web views run on Chromium. Whether
 * Chromium can run is asked each time one is needed, not as the app starts: the web view is always offered, and a page
 * that can't open says so. The Chromium is the one the app's Java runtime carries.
 */
@BindingContainer
interface WebViewBindings {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun chromiumCookieMirror(): ChromiumCookieMirror = ChromiumCookieMirror(AppCookieStore.store)

        @Provides
        @SingleIn(AppScope::class)
        fun jcefRuntime(
            host: DesktopJcefHost,
            cookies: ChromiumCookieMirror,
        ): JcefRuntime = JbrJcefRuntime(host, cookies)

        @Provides
        @SingleIn(AppScope::class)
        fun siteOrigins(): SiteOrigins = SiteOrigins()

        @Provides
        @SingleIn(AppScope::class)
        fun browserView(
            runtime: JcefRuntime,
            cookies: ChromiumCookieMirror,
            origins: SiteOrigins,
            linkOpener: LinkOpener,
            clipboard: TextClipboard,
        ): BrowserView = ChromiumBrowserView(runtime, cookies, origins, linkOpener, clipboard)

        @Provides
        @SingleIn(AppScope::class)
        fun webViewDestination(): WebViewDestination = WindowWebViewDestination()

        @Provides
        @SingleIn(AppScope::class)
        fun challengePage(runtime: JcefRuntime, cookies: ChromiumCookieMirror): ChallengePage =
            ChromiumChallengePage(runtime, cookies)

        // A page an extension doesn't give a user agent of its own sends the one the app's requests send
        @Provides
        @SingleIn(AppScope::class)
        fun extensionPages(
            runtime: JcefRuntime,
            cookies: ChromiumCookieMirror,
            network: Lazy<NetworkHelper>,
        ): ExtensionPages = ChromiumExtensionPages(runtime, cookies) { network.value.defaultUserAgentProvider() }

        // A heavy page's renderer peaks near a gigabyte here, so fewer are kept than on a phone
        @Provides
        @SingleIn(AppScope::class)
        fun webPages(
            pages: ExtensionPages,
            messages: WebViewInterceptorMessages,
            profiles: ProfileStore,
            @AppCoroutineScope scope: CoroutineScope,
        ): WebPages = WebPageKeeper(
            hostPages = DesktopHostPages(pages, messages),
            limits = WebPageLimits(perSource = 1, total = 2),
            activeProfileId = { profiles.currentProfileId },
            scope = scope,
        )

        @Provides
        @SingleIn(AppScope::class)
        fun webViewData(runtime: JcefRuntime, host: DesktopJcefHost, origins: SiteOrigins): WebViewData =
            ChromiumWebViewData(runtime, host, origins)
    }
}
