package app.katari.desktop.di

import app.katari.core.common.network.cookie.AppCookieStore
import app.katari.core.common.web.ChallengePage
import app.katari.core.common.web.ExtensionPages
import app.katari.core.platform.clipboard.TextClipboard
import app.katari.core.platform.link.LinkOpener
import app.katari.desktop.webview.DesktopJcefHost
import app.katari.desktop.webview.NoChallengePage
import app.katari.desktop.webview.NoExtensionPages
import app.katari.desktop.webview.NoWebViewData
import app.katari.desktop.webview.NoWebViewDestination
import app.katari.desktop.webview.WindowWebViewDestination
import app.katari.webview.jcef.cookie.ChromiumCookieMirror
import app.katari.webview.jcef.data.ChromiumWebViewData
import app.katari.webview.jcef.data.SiteOrigins
import app.katari.webview.jcef.page.challenge.ChromiumChallengePage
import app.katari.webview.jcef.page.extension.ChromiumExtensionPages
import app.katari.webview.jcef.runtime.JbrJcefRuntime
import app.katari.webview.jcef.runtime.JcefPresence
import app.katari.webview.jcef.runtime.JcefRuntime
import app.katari.webview.jcef.surface.ChromiumBrowserView
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.more.settings.screen.advanced.WebViewData
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import eu.kanade.tachiyomi.ui.webview.browser.BrowserView

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
