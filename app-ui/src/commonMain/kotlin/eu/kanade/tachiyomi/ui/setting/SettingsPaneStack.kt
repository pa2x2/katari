package eu.kanade.tachiyomi.ui.setting

import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.presentation.more.settings.screen.SettingsAppearanceScreen
import eu.kanade.presentation.more.settings.screen.SettingsMainScreen

/**
 * The settings stack as the other layout shows it, with the same screens so the open page keeps its state: one pane
 * keeps the list at the bottom, under the open page; two panes show the list beside the page, so their stack holds
 * only pages, starting from the first page when none was open.
 */
internal fun settingsStackFor(twoPane: Boolean, stack: List<Screen>): List<Screen> {
    val pages = stack.filterNot { it is SettingsMainScreen }
    return if (twoPane) pages.ifEmpty { listOf(SettingsAppearanceScreen()) } else listOf(SettingsMainScreen()) + pages
}
