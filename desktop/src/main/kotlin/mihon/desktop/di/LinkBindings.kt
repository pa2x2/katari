package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import mihon.core.platform.link.LinkOpener
import mihon.desktop.link.XdgLinkOpener
import mihon.desktop.platform.DesktopOperatingSystem

@BindingContainer
object LinkBindings {
    @Provides
    fun linkOpener(operatingSystem: DesktopOperatingSystem): LinkOpener = when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> XdgLinkOpener()
    }
}
