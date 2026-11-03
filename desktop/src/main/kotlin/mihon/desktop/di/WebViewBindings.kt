package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.more.settings.screen.advanced.WebViewData
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import eu.kanade.tachiyomi.ui.webview.browser.BrowserView
import mihon.core.common.network.cookie.AppCookieStore
import mihon.core.common.web.ChallengePage
import mihon.core.common.web.ExtensionPages
import mihon.core.platform.clipboard.TextClipboard
import mihon.core.platform.link.LinkOpener
import mihon.desktop.webview.DesktopJcefHost
import mihon.desktop.webview.NoChallengePage
import mihon.desktop.webview.NoExtensionPages
import mihon.desktop.webview.NoWebViewData
import mihon.desktop.webview.NoWebViewDestination
import mihon.desktop.webview.WindowWebViewDestination
import mihon.webview.jcef.cookie.ChromiumCookieMirror
import mihon.webview.jcef.data.ChromiumWebViewData
import mihon.webview.jcef.data.SiteOrigins
import mihon.webview.jcef.page.challenge.ChromiumChallengePage
import mihon.webview.jcef.page.extension.ChromiumExtensionPages
import mihon.webview.jcef.runtime.JbrJcefRuntime
import mihon.webview.jcef.runtime.JcefPresence
import mihon.webview.jcef.runtime.JcefRuntime
import mihon.webview.jcef.surface.ChromiumBrowserView

/**
 * The web view, the pages challenges are solved in and the pages extensions' own web views run on Chromium, where the
 * Java runtime carries JCEF; on any other runtime the web view and its data stay hidden behind the stand-ins,
 * challenges go back to sources unsolved and extensions' web views fail. The browser view is bound either way, as
 * nothing opens a page while the web view is hidden.
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
        fun webViewDestination(runtime: JcefRuntime): WebViewDestination =
            if (runtime.presence is JcefPresence.Present) WindowWebViewDestination() else NoWebViewDestination()

        @Provides
        @SingleIn(AppScope::class)
        fun challengePage(runtime: JcefRuntime, cookies: ChromiumCookieMirror): ChallengePage =
            if (runtime.presence is JcefPresence.Present) {
                ChromiumChallengePage(runtime, cookies)
            } else {
                NoChallengePage()
            }

        // A page an extension doesn't give a user agent of its own sends the one the app's requests send
        @Provides
        @SingleIn(AppScope::class)
        fun extensionPages(
            runtime: JcefRuntime,
            cookies: ChromiumCookieMirror,
            network: Lazy<NetworkHelper>,
        ): ExtensionPages {
            val userAgent = { network.value.defaultUserAgentProvider() }
            return if (runtime.presence is JcefPresence.Present) {
                ChromiumExtensionPages(runtime, cookies, userAgent)
            } else {
                NoExtensionPages(userAgent)
            }
        }

        @Provides
        @SingleIn(AppScope::class)
        fun webViewData(runtime: JcefRuntime, host: DesktopJcefHost, origins: SiteOrigins): WebViewData =
            if (runtime.presence is JcefPresence.Present) {
                ChromiumWebViewData(runtime, host, origins)
            } else {
                NoWebViewData()
            }
    }
}
