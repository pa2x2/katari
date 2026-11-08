package app.katari.desktop.di

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.core.platform.locale.AppLocaleStore
import app.katari.desktop.appearance.PortalClockFormat
import app.katari.desktop.appearance.PortalSettings
import app.katari.desktop.appearance.PreferenceAppLocaleStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.util.SystemClockFormat

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
