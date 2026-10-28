package mihon.desktop.appearance

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.appearance.AppUiLanguage
import java.util.Locale

/** Desktop shows the app in the system's language, which the JVM takes as its default locale. */
@Inject
class DesktopAppUiLanguage : AppUiLanguage {
    override fun locale(): Locale = Locale.getDefault()
}
