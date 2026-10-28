package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.appearance.AndroidAppUiLanguage
import eu.kanade.presentation.more.settings.screen.appearance.AppUiLanguage

@BindingContainer
interface LanguageBindings {
    @Binds
    val AndroidAppUiLanguage.appUiLanguage: AppUiLanguage
}
