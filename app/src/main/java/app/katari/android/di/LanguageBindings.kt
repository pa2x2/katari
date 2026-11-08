package app.katari.android.di

import app.katari.android.locale.AppCompatLocaleStore
import app.katari.android.time.DateFormatSystemClockFormat
import app.katari.core.platform.locale.AppLocaleStore
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.util.SystemClockFormat

@BindingContainer
interface LanguageBindings {
    @Binds
    val AppCompatLocaleStore.appLocaleStore: AppLocaleStore

    @Binds
    val DateFormatSystemClockFormat.systemClockFormat: SystemClockFormat
}
