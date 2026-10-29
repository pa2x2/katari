package mihon.app.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dev.zacsweers.metro.Inject
import mihon.core.platform.locale.AppLocaleStore
import java.util.Locale

/** The language AppCompat keeps for the app, or else the system's first. */
@Inject
class AppCompatLocaleStore : AppLocaleStore {
    override val current: Locale?
        get() = AppCompatDelegate.getApplicationLocales().get(0) ?: LocaleListCompat.getAdjustedDefault().get(0)
}
