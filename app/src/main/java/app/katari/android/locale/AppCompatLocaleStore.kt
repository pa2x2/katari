package app.katari.android.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.katari.core.platform.locale.AppLocaleStore
import dev.zacsweers.metro.Inject
import java.util.Locale

/**
 * The per-app language AppCompat keeps, or else the system's first. AppCompat recreates the open activities in a
 * language it is given, so a pick applies at once.
 */
@Inject
class AppCompatLocaleStore : AppLocaleStore {
    override val current: Locale?
        get() = AppCompatDelegate.getApplicationLocales().get(0) ?: LocaleListCompat.getAdjustedDefault().get(0)

    override val picked: Locale?
        get() = AppCompatDelegate.getApplicationLocales().get(0)

    override val appliesAtOnce: Boolean = true

    override fun set(languageTag: String?) {
        val locales = if (languageTag == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    override fun restartToApply() = error("A language picked on Android applies at once")
}
