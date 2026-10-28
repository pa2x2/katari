package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.more.settings.screen.advanced.DisplayBackendSetting
import eu.kanade.presentation.more.settings.screen.appearance.InterfaceSizeSetting
import mihon.desktop.display.backend.DesktopDisplayBackendSetting
import mihon.desktop.display.backend.DisplayBackend
import mihon.desktop.display.backend.LinuxDisplayBackend
import mihon.desktop.display.size.DesktopInterfaceSizeSetting
import mihon.desktop.display.size.InterfaceSizePreferences
import mihon.desktop.display.size.SessionScale
import mihon.desktop.display.size.XftSessionScale
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.feature.profiles.core.ProfileStore

@BindingContainer
interface DisplayBindings {
    @Binds
    val DesktopInterfaceSizeSetting.interfaceSizeSetting: InterfaceSizeSetting

    @Binds
    val DesktopDisplayBackendSetting.displayBackendSetting: DisplayBackendSetting

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun displayBackend(operatingSystem: DesktopOperatingSystem): DisplayBackend = when (operatingSystem) {
            DesktopOperatingSystem.LINUX -> LinuxDisplayBackend()
        }

        @Provides
        @SingleIn(AppScope::class)
        fun sessionScale(operatingSystem: DesktopOperatingSystem, backend: DisplayBackend): SessionScale =
            when (operatingSystem) {
                DesktopOperatingSystem.LINUX -> XftSessionScale(backend)
            }

        @Provides
        @SingleIn(AppScope::class)
        fun interfaceSizePreferences(profileStore: ProfileStore): InterfaceSizePreferences =
            InterfaceSizePreferences(profileStore.profileStore())
    }
}
