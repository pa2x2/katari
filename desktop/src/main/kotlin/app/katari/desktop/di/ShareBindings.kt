package app.katari.desktop.di

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.core.platform.share.ImageSharing
import app.katari.core.platform.share.LinkSharing
import app.katari.core.platform.share.TextSharing
import app.katari.desktop.share.NoImageSharing
import app.katari.desktop.share.NoLinkSharing
import app.katari.desktop.share.NoTextSharing
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

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
