package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.appearance.AndroidAppThemeHost
import eu.kanade.presentation.more.settings.screen.appearance.AppThemeHost

@BindingContainer
interface ThemeBindings {
    @Binds
    val AndroidAppThemeHost.appThemeHost: AppThemeHost
}
