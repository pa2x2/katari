package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.util.SystemClockFormat
import mihon.core.platform.locale.AppLocaleStore
import mihon.desktop.appearance.PortalClockFormat
import mihon.desktop.appearance.PortalSettings
import mihon.desktop.appearance.PreferenceAppLocaleStore
import mihon.desktop.platform.DesktopOperatingSystem

@BindingContainer
interface LanguageBindings {
    @Binds
    val PreferenceAppLocaleStore.appLocaleStore: AppLocaleStore

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun systemClockFormat(
            operatingSystem: DesktopOperatingSystem,
            portalSettings: Lazy<PortalSettings>,
        ): SystemClockFormat = when (operatingSystem) {
            DesktopOperatingSystem.LINUX -> PortalClockFormat(portalSettings.value)
        }
    }
}
