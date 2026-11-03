package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.more.settings.screen.advanced.DisplayBackendSetting
import eu.kanade.presentation.more.settings.screen.appearance.InterfaceSizeSetting
import mihon.desktop.application.DesktopAppVariant
import mihon.desktop.display.backend.DesktopDisplayBackendSetting
import mihon.desktop.display.backend.DisplayBackend
import mihon.desktop.display.backend.LinuxDisplayBackend
import mihon.desktop.display.size.DesktopInterfaceSizeSetting
import mihon.desktop.display.size.InterfaceSizePreferences
import mihon.desktop.display.size.SessionScale
import mihon.desktop.display.size.XftSessionScale
import mihon.desktop.platform.DesktopOperatingSystem
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller

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
            DesktopOperatingSystem.LINUX -> LinuxDisplayBackend(DesktopAppVariant.current.applicationId)
        }

        @Provides
        @SingleIn(AppScope::class)
        fun sessionScale(operatingSystem: DesktopOperatingSystem, backend: DisplayBackend): SessionScale =
            when (operatingSystem) {
                DesktopOperatingSystem.LINUX -> XftSessionScale(backend)
            }

        @Provides
        @SingleIn(AppScope::class)
        fun interfaceSizePreferencesOwner(
            owners: ProfilePreferenceOwnerInstaller,
        ): InstalledProfilePreferenceOwner<InterfaceSizePreferences> =
            owners.register(
                id = ProfilePreferenceOwnerId("desktop.interface-size"),
                factory = ::InterfaceSizePreferences,
            )

        @Provides
        @IntoSet
        fun interfaceSizePreferencesOwnerRegistration(
            owner: InstalledProfilePreferenceOwner<InterfaceSizePreferences>,
        ): InstalledProfilePreferenceOwner<*> = owner

        @Provides
        @SingleIn(AppScope::class)
        fun interfaceSizePreferences(
            owner: InstalledProfilePreferenceOwner<InterfaceSizePreferences>,
        ): InterfaceSizePreferences = owner.create()
    }
}
