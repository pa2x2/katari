package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.appearance.AndroidAppUiLanguage
import eu.kanade.presentation.more.settings.screen.appearance.AppUiLanguage
import mihon.app.locale.AppCompatLocaleStore
import mihon.core.platform.locale.AppLocaleStore

@BindingContainer
interface LanguageBindings {
    @Binds
    val AndroidAppUiLanguage.appUiLanguage: AppUiLanguage

    @Binds
    val AppCompatLocaleStore.appLocaleStore: AppLocaleStore
}
