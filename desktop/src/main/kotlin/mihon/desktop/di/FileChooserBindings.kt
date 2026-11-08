package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.core.common.platform.DesktopOperatingSystem
import mihon.desktop.filechooser.portal.PortalFileChooser
import mihon.desktop.filechooser.toolkit.ToolkitFileChooser
import mihon.presentation.kit.files.SystemFileChooser

@BindingContainer
object FileChooserBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun systemFileChooser(operatingSystem: DesktopOperatingSystem): SystemFileChooser = when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> PortalFileChooser(fallback = ToolkitFileChooser())
    }
}
