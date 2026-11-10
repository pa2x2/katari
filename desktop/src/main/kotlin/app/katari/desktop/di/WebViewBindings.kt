package app.katari.desktop.di

import app.katari.core.common.network.cookie.AppCookieStore
import app.katari.core.common.web.ChallengePage
import app.katari.core.common.web.ExtensionPages
import app.katari.core.metro.AppCoroutineScope
import app.katari.core.platform.clipboard.TextClipboard
import app.katari.core.platform.link.LinkOpener
import app.katari.core.platform.network.NetworkConnectivity
import app.katari.desktop.application.DesktopBuildInfo
import app.katari.desktop.storage.DesktopDirectories
import app.katari.desktop.webview.DesktopJcefHost
import app.katari.desktop.webview.WindowWebViewDestination
import app.katari.desktop.webview.setup.DesktopWebViewSetup
import app.katari.desktop.webview.setup.NoticedChallengePage
import app.katari.desktop.webview.setup.NoticedExtensionPages
import app.katari.model.artifacts.runtime.store.modelArtifactStore
import app.katari.profiles.core.ProfileStore
import app.katari.webpages.host.DesktopHostPages
import app.katari.webpages.keeper.WebPageKeeper
import app.katari.webpages.keeper.WebPageLimits
import app.katari.webview.chromium.ChromiumInstallation
import app.katari.webview.chromium.ChromiumRelease
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
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.more.settings.screen.advanced.WebViewData
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.interceptor.WebViewInterceptorMessages
import eu.kanade.tachiyomi.source.entry.webpage.WebPages
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import eu.kanade.tachiyomi.ui.webview.browser.BrowserView
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The web view, the pages challenges are solved in and the pages extensions' own web views run on Chromium. Whether
 * Chromium can run is asked each time one is needed, not as the app starts: the web view is always offered, and a page
 * that can't open says so. The Chromium is one the app downloads into its data folder.
 */
@BindingContainer
interface WebViewBindings {
    @Binds
    val DesktopWebViewSetup.webViewSetup: WebViewSetup

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun chromiumCookieMirror(): ChromiumCookieMirror = ChromiumCookieMirror(AppCookieStore.store)

        // Its download has a store of its own, so it never shows among the models the app downloads
        @Provides
        @SingleIn(AppScope::class)
        fun chromiumInstallation(
            directories: DesktopDirectories,
            connectivity: NetworkConnectivity,
            network: Lazy<NetworkHelper>,
        ): ChromiumInstallation = ChromiumInstallation(
            release = ChromiumRelease(
                version = DesktopBuildInfo.CHROMIUM_VERSION,
                archiveUrl = DesktopBuildInfo.CHROMIUM_ARCHIVE_URL,
                archiveSha256 = DesktopBuildInfo.CHROMIUM_ARCHIVE_SHA256,
                archiveBytes = DesktopBuildInfo.CHROMIUM_ARCHIVE_BYTES,
                installedBytes = DesktopBuildInfo.CHROMIUM_INSTALLED_BYTES,
            ),
            root = directories.data.resolve(CHROMIUM_DIRECTORY_NAME),
            store = { downloads -> modelArtifactStore({ downloads }, connectivity) { network.value.client } },
            scope = CoroutineScope(SupervisorJob()),
            io = Dispatchers.IO,
        )

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
        fun challengePage(
            runtime: JcefRuntime,
            cookies: ChromiumCookieMirror,
            setup: DesktopWebViewSetup,
        ): ChallengePage = NoticedChallengePage(ChromiumChallengePage(runtime, cookies), setup::neededWithoutScreen)

        // A page an extension doesn't give a user agent of its own sends the one the app's requests send
        @Provides
        @SingleIn(AppScope::class)
        fun extensionPages(
            runtime: JcefRuntime,
            cookies: ChromiumCookieMirror,
            network: Lazy<NetworkHelper>,
            setup: DesktopWebViewSetup,
        ): ExtensionPages = NoticedExtensionPages(
            ChromiumExtensionPages(runtime, cookies) { network.value.defaultUserAgentProvider() },
            setup::neededWithoutScreen,
        )

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

private const val CHROMIUM_DIRECTORY_NAME = "chromium"
