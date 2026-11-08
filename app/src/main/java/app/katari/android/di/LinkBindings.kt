package app.katari.android.di

import app.katari.android.link.AndroidLinkOpener
import app.katari.core.platform.link.LinkOpener
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds

@BindingContainer
interface LinkBindings {
    @Binds
    val AndroidLinkOpener.linkOpener: LinkOpener
}
