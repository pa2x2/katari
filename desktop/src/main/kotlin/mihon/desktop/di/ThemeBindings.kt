package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.appearance.AppThemeHost
import mihon.desktop.appearance.DesktopAppThemeHost

@BindingContainer
interface ThemeBindings {
    @Binds
    val DesktopAppThemeHost.appThemeHost: AppThemeHost
}
