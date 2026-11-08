package app.katari.android.di

import app.katari.android.clipboard.AndroidTextClipboard
import app.katari.core.platform.clipboard.TextClipboard
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds

@BindingContainer
interface ClipboardBindings {
    @Binds
    val AndroidTextClipboard.textClipboard: TextClipboard
}
