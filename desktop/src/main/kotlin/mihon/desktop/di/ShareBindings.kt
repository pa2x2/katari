package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import mihon.core.platform.share.ImageSharing
import mihon.core.platform.share.LinkSharing
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.desktop.share.NoImageSharing
import mihon.desktop.share.NoLinkSharing

@BindingContainer
object ShareBindings {
    @Provides
    fun imageSharing(operatingSystem: DesktopOperatingSystem): ImageSharing = when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> NoImageSharing()
    }

    @Provides
    fun linkSharing(operatingSystem: DesktopOperatingSystem): LinkSharing = when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> NoLinkSharing()
    }
}
