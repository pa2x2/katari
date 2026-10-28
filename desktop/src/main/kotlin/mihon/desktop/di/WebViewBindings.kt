package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.webview.NoEntryWebViewFeature
import mihon.entry.interactions.source.EntryWebViewFeature

@BindingContainer
interface WebViewBindings {
    @Binds
    val NoEntryWebViewFeature.entryWebViewFeature: EntryWebViewFeature
}
