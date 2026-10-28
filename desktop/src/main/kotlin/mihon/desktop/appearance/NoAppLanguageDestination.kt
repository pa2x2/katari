package mihon.desktop.appearance

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageDestination
import eu.kanade.presentation.util.Screen

/** Desktop shows the app in the system's language and offers no language of its own, so the setting is hidden. */
@Inject
class NoAppLanguageDestination : AppLanguageDestination {
    override val isAvailable: Boolean = false

    override fun screen(): Screen = error("An app language of its own is not available on desktop")
}
