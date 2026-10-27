package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.app.clipboard.AndroidTextClipboard
import mihon.core.platform.clipboard.TextClipboard

@BindingContainer
interface ClipboardBindings {
    @Binds
    val AndroidTextClipboard.textClipboard: TextClipboard
}
