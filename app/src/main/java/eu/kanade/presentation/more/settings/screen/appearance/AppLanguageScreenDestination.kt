package eu.kanade.presentation.more.settings.screen.appearance

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen

@Inject
class AppLanguageScreenDestination : AppLanguageDestination {
    override val isAvailable: Boolean = true

    override fun screen(): Screen = AppLanguageScreen()
}
