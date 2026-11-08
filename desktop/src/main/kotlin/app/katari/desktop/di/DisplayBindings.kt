package app.katari.desktop.di

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.desktop.application.DesktopAppVariant
import app.katari.desktop.display.backend.DisplayBackend
import app.katari.desktop.display.backend.LinuxDisplayBackend
import app.katari.desktop.display.size.DesktopInterfaceSizeSetting
import app.katari.desktop.display.size.InterfaceSizePreferences
import app.katari.desktop.display.size.SessionScale
import app.katari.desktop.display.size.XftSessionScale
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.more.settings.screen.appearance.InterfaceSizeSetting
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller

@BindingContainer
interface DisplayBindings {
    @Binds
    val DesktopInterfaceSizeSetting.interfaceSizeSetting: InterfaceSizeSetting

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun displayBackend(operatingSystem: DesktopOperatingSystem): DisplayBackend = when (operatingSystem) {
            DesktopOperatingSystem.LINUX -> LinuxDisplayBackend(DesktopAppVariant.current.applicationId)
        }

        @Provides
        @SingleIn(AppScope::class)
        fun sessionScale(operatingSystem: DesktopOperatingSystem): SessionScale = when (operatingSystem) {
            DesktopOperatingSystem.LINUX -> XftSessionScale()
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
