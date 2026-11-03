package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.presentation.more.settings.screen.appearance.AppThemeHost
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.desktop.appearance.DesktopAppThemeHost
import mihon.desktop.appearance.FontconfigInterfaceFont
import mihon.desktop.appearance.PortalAccentColor
import mihon.desktop.appearance.PortalColorScheme
import mihon.desktop.appearance.PortalSettings
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.presentation.kit.theme.SystemAccentColor
import mihon.presentation.kit.theme.SystemColorScheme
import mihon.presentation.kit.theme.SystemInterfaceFont

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
