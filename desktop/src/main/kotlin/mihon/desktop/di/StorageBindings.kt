package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.desktop.storage.DesktopDirectories
import mihon.desktop.storage.systemDesktopDirectories

@BindingContainer
object StorageBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun desktopDirectories(operatingSystem: DesktopOperatingSystem): DesktopDirectories =
        systemDesktopDirectories(operatingSystem)
}
