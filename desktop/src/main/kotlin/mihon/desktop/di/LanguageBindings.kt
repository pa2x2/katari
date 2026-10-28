package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.appearance.AppUiLanguage
import mihon.desktop.appearance.DesktopAppUiLanguage

@BindingContainer
interface LanguageBindings {
    @Binds
    val DesktopAppUiLanguage.appUiLanguage: AppUiLanguage
}
