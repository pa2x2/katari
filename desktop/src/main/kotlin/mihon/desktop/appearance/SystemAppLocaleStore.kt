package mihon.desktop.appearance

import dev.zacsweers.metro.Inject
import mihon.core.platform.locale.AppLocaleStore
import java.util.Locale

/** Desktop shows the app in the system's language, which the JVM takes as its default locale. */
@Inject
class SystemAppLocaleStore : AppLocaleStore {
    override val current: Locale
        get() = Locale.getDefault()
}
