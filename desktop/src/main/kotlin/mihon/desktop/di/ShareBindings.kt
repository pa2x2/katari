package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import mihon.core.common.platform.DesktopOperatingSystem
import mihon.core.platform.share.ImageSharing
import mihon.core.platform.share.LinkSharing
import mihon.core.platform.share.TextSharing
import mihon.desktop.share.NoImageSharing
import mihon.desktop.share.NoLinkSharing
import mihon.desktop.share.NoTextSharing

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

    @Provides
    fun textSharing(operatingSystem: DesktopOperatingSystem): TextSharing = when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> NoTextSharing()
    }
}
