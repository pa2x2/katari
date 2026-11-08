package app.katari.desktop.di

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.desktop.filechooser.portal.PortalFileChooser
import app.katari.desktop.filechooser.toolkit.ToolkitFileChooser
import app.katari.presentation.kit.files.SystemFileChooser
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@BindingContainer
object FileChooserBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun systemFileChooser(operatingSystem: DesktopOperatingSystem): SystemFileChooser = when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> PortalFileChooser(fallback = ToolkitFileChooser())
    }
}
