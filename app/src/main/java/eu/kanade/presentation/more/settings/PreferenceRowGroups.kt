package eu.kanade.presentation.more.settings

import mihon.presentation.kit.list.RowGroupPosition

/**
 * The card slice each preference draws, or null for one that sits outside the cards. Consecutive rows share a card;
 * a group header or an info line ends it. Positions count only visible rows, so hiding the first or last row of a card
 * moves the rounded corners to its neighbour.
 */
internal fun rowGroupPositions(preferences: List<Preference>): List<RowGroupPosition?> {
    val positions = MutableList<RowGroupPosition?>(preferences.size) { null }
    var start = 0
    while (start < preferences.size) {
        if (!preferences[start].isCardRow()) {
            start++
            continue
        }
        var end = start
        while (end < preferences.size && preferences[end].isCardRow()) end++
        val visible = (start..<end).filter { preferences[it].visible }
        visible.forEachIndexed { i, index -> positions[index] = RowGroupPosition.of(i, visible.size) }
        // A hidden row still composes while it animates out; it keeps a plain slice so no corner jumps onto it.
        (start..<end).filterNot { preferences[it].visible }.forEach { positions[it] = RowGroupPosition.Middle }
        start = end
    }
    return positions
}

private fun Preference.isCardRow(): Boolean =
    this is Preference.PreferenceItem<*, *> &&
        this !is Preference.PreferenceItem.InfoPreference &&
        (this !is Preference.PreferenceItem.CustomPreference || framed)
