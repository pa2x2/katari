package eu.kanade.presentation.more.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEachIndexed
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.widget.PreferenceGroupHeader
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Preference Screen composable which contains a list of [Preference] items
 * @param items [Preference] items which should be displayed on the preference screen. An item can be a single [PreferenceItem] or a group ([Preference.PreferenceGroup])
 * @param modifier [Modifier] to be applied to the preferenceScreen layout
 */
@Composable
fun PreferenceScreen(
    items: List<Preference>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state = rememberLazyListState()
    val highlightKey = SearchableSettings.highlightKey
    val profileStore = LocalAppUiGraph.current.profileStore
    val allItemsProfileSpecific = items.isNotEmpty() && items.all { it.belongsToProfile(profileStore) }
    if (highlightKey != null) {
        LaunchedEffect(Unit) {
            val i = items.findHighlightedIndex(highlightKey)
            if (i >= 0) {
                delay(0.5.seconds)
                state.animateScrollToItem(i)
            }
            SearchableSettings.highlightKey = null
        }
    }

    val topLevelPositions = rowGroupPositions(items)

    RowList(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
    ) {
        // Made on each run of this block, which the list may run again for the same items
        val keys = PreferenceItemKeys()
        items.fastForEachIndexed { i, preference ->
            when (preference) {
                // Create Preference Group
                is Preference.PreferenceGroup -> {
                    if (!preference.visible) return@fastForEachIndexed
                    val showGroupChip = !allItemsProfileSpecific && preference.belongsToProfile(profileStore)
                    val groupKey = keys.of(preference)
                    val itemKeys = preference.preferenceItems.map(keys::of)

                    item(key = groupKey) {
                        Column {
                            PreferenceGroupHeader(
                                title = preference.title,
                                isProfileSpecific = showGroupChip,
                                highlighted = preference.title == highlightKey,
                            )
                        }
                    }
                    val positions = rowGroupPositions(preference.preferenceItems)
                    itemsIndexed(preference.preferenceItems, key = { index, _ -> itemKeys[index] }) { index, item ->
                        PreferenceItem(
                            item = item,
                            highlightKey = highlightKey,
                            showProfileChip = !allItemsProfileSpecific && !showGroupChip &&
                                item.belongsToProfile(profileStore),
                            groupPosition = positions[index],
                        )
                    }
                    item(key = "gap:$groupKey") { RowGroupGap() }
                }

                // Create Preference Item
                is Preference.PreferenceItem<*, *> -> item(key = keys.of(preference)) {
                    PreferenceItem(
                        item = preference,
                        highlightKey = highlightKey,
                        showProfileChip = !allItemsProfileSpecific && preference.belongsToProfile(profileStore),
                        groupPosition = topLevelPositions[i],
                    )
                }
            }
        }
        // Every group ends with a gap, the last one too, so the list's last card keeps clear of the window's bottom
        // edge; a list that ends on rows outside a group gets the same gap
        if (items.lastOrNull { it.visible } is Preference.PreferenceItem<*, *>) {
            item(key = END_GAP_KEY) { RowGroupGap() }
        }
    }
}

private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    return flatMap {
        if (it is Preference.PreferenceGroup) {
            buildList<String?> {
                add(it.title) // Header
                addAll(it.preferenceItems.map { groupItem -> groupItem.title })
                add(null) // Spacer
            }
        } else {
            listOf(it.title)
        }
    }.indexOfFirst { it == highlightKey }
}

private const val END_GAP_KEY = "end-gap"
