package eu.kanade.presentation.more.settings.screen.textrules

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen

@Inject
class BookTextRulesScreenDestination : BookTextRulesDestination {
    override val isAvailable: Boolean = true

    override fun screen(): Screen = BookTextRulesScreen()
}
