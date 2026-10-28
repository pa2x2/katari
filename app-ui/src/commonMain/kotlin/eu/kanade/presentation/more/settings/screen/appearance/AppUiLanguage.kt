package eu.kanade.presentation.more.settings.screen.appearance

import java.util.Locale

/** The language the app's own text is shown in: the one picked through [AppLanguageDestination], or the system's. */
fun interface AppUiLanguage {
    fun locale(): Locale
}
