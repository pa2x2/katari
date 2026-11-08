package app.katari.desktop.di

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.core.metro.AppCoroutineScope
import app.katari.desktop.appearance.DesktopAppThemeHost
import app.katari.desktop.appearance.FontconfigInterfaceFont
import app.katari.desktop.appearance.PortalAccentColor
import app.katari.desktop.appearance.PortalColorScheme
import app.katari.desktop.appearance.PortalSettings
import app.katari.presentation.kit.theme.SystemAccentColor
import app.katari.presentation.kit.theme.SystemColorScheme
import app.katari.presentation.kit.theme.SystemInterfaceFont
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.more.settings.screen.appearance.AppThemeHost
import kotlinx.coroutines.CoroutineScope

@BindingContainer
interface ThemeBindings {
    @Binds
    val DesktopAppThemeHost.appThemeHost: AppThemeHost

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun portalSettings(@AppCoroutineScope scope: CoroutineScope): PortalSettings = PortalSettings(scope)

        @Provides
        @SingleIn(AppScope::class)
        fun systemColorScheme(
            operatingSystem: DesktopOperatingSystem,
            portalSettings: Lazy<PortalSettings>,
            @AppCoroutineScope scope: CoroutineScope,
        ): SystemColorScheme = when (operatingSystem) {
            DesktopOperatingSystem.LINUX -> PortalColorScheme(portalSettings.value, scope)
        }

        @Provides
        @SingleIn(AppScope::class)
        fun systemAccentColor(
            operatingSystem: DesktopOperatingSystem,
            portalSettings: Lazy<PortalSettings>,
            @AppCoroutineScope scope: CoroutineScope,
        ): SystemAccentColor = when (operatingSystem) {
            DesktopOperatingSystem.LINUX -> PortalAccentColor(portalSettings.value, scope)
        }

        @Provides
        @SingleIn(AppScope::class)
        fun systemInterfaceFont(operatingSystem: DesktopOperatingSystem): SystemInterfaceFont = when (operatingSystem) {
            DesktopOperatingSystem.LINUX -> FontconfigInterfaceFont()
        }
    }
}
