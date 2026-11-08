package app.katari.desktop.di

import app.katari.core.platform.clipboard.ImageClipboard
import app.katari.core.platform.clipboard.TextClipboard
import app.katari.desktop.clipboard.AwtImageClipboard
import app.katari.desktop.clipboard.AwtTextClipboard
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds

@BindingContainer
interface ClipboardBindings {
    @Binds
    val AwtTextClipboard.textClipboard: TextClipboard

    @Binds
    val AwtImageClipboard.imageClipboard: ImageClipboard
}
