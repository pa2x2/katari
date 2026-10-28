package eu.kanade.presentation.more.settings.screen.appearance

import androidx.appcompat.app.AppCompatDelegate
import dev.zacsweers.metro.Inject
import java.util.Locale

/** AppCompat's per-app language, which [AppLanguageScreen] sets, or the system's when none is set. */
@Inject
class AndroidAppUiLanguage : AppUiLanguage {
    override fun locale(): Locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
}
