package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import mihon.core.platform.share.ImageSharing
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.desktop.share.NoImageSharing

@BindingContainer
object ShareBindings {
    @Provides
    fun imageSharing(operatingSystem: DesktopOperatingSystem): ImageSharing = when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> NoImageSharing()
    }
}
