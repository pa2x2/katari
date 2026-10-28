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
import mihon.desktop.appearance.PortalColorScheme
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.presentation.kit.theme.SystemColorScheme

@BindingContainer
interface ThemeBindings {
    @Binds
    val DesktopAppThemeHost.appThemeHost: AppThemeHost

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun systemColorScheme(
            operatingSystem: DesktopOperatingSystem,
            @AppCoroutineScope scope: CoroutineScope,
        ): SystemColorScheme = when (operatingSystem) {
            DesktopOperatingSystem.LINUX -> PortalColorScheme(scope)
        }
    }
}
