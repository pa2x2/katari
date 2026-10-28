package mihon.desktop.book

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.textrules.BookTextRulesDestination
import eu.kanade.presentation.util.Screen

/**
 * The book text rules screen isn't shared with desktop yet, so the book reader settings hide their row for it. Stands
 * in until desktop hosts that screen.
 */
@Inject
class NoBookTextRulesDestination : BookTextRulesDestination {
    override val isAvailable: Boolean = false

    override fun screen(): Screen = error("The book text rules screen is not available on desktop yet")
}
