package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.util.SystemClockFormat
import mihon.app.locale.AppCompatLocaleStore
import mihon.app.time.DateFormatSystemClockFormat
import mihon.core.platform.locale.AppLocaleStore

@BindingContainer
interface LanguageBindings {
    @Binds
    val AppCompatLocaleStore.appLocaleStore: AppLocaleStore

    @Binds
    val DateFormatSystemClockFormat.systemClockFormat: SystemClockFormat
}
