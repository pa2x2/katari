package eu.kanade.presentation.more.settings.screen.textrules

import eu.kanade.presentation.util.Screen

/** The screen that edits the rules the book reader applies to a book's text. */
interface BookTextRulesDestination {
    /** False where the platform doesn't host the rules screen yet; the book reader's row for it is then hidden. */
    val isAvailable: Boolean

    /** Only called while [isAvailable]. */
    fun screen(): Screen
}
