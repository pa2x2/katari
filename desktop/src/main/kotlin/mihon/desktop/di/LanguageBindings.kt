package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.appearance.AppUiLanguage
import mihon.core.platform.locale.AppLocaleStore
import mihon.desktop.appearance.DesktopAppUiLanguage
import mihon.desktop.appearance.SystemAppLocaleStore

@BindingContainer
interface LanguageBindings {
    @Binds
    val DesktopAppUiLanguage.appUiLanguage: AppUiLanguage

    @Binds
    val SystemAppLocaleStore.appLocaleStore: AppLocaleStore
}
