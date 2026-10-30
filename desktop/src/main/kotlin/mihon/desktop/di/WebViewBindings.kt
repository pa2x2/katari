package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.core.common.network.cookie.AppCookieStore
import mihon.desktop.webview.DesktopJcefHost
import mihon.webview.jcef.cookie.ChromiumCookieMirror
import mihon.webview.jcef.runtime.JbrJcefRuntime
import mihon.webview.jcef.runtime.JcefRuntime

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
    }
}
