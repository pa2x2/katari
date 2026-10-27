package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.core.platform.clipboard.TextClipboard
import mihon.desktop.clipboard.AwtTextClipboard

@BindingContainer
interface ClipboardBindings {
    @Binds
    val AwtTextClipboard.textClipboard: TextClipboard
}
