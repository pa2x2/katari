package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.app.link.AndroidLinkOpener
import mihon.core.platform.link.LinkOpener

@BindingContainer
interface LinkBindings {
    @Binds
    val AndroidLinkOpener.linkOpener: LinkOpener
}
