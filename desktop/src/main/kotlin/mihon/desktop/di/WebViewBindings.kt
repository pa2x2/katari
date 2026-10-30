package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.desktop.webview.DesktopJcefHost
import mihon.desktop.webview.NoEntryWebViewFeature
import mihon.entry.interactions.source.EntryWebViewFeature
import mihon.webview.jcef.runtime.JbrJcefRuntime
import mihon.webview.jcef.runtime.JcefRuntime

@BindingContainer
interface WebViewBindings {
    @Binds
    val NoEntryWebViewFeature.entryWebViewFeature: EntryWebViewFeature

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun jcefRuntime(host: DesktopJcefHost): JcefRuntime = JbrJcefRuntime(host)
    }
}
