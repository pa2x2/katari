package eu.kanade.presentation.more.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEachIndexed
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.widget.PreferenceGroupHeader
import kotlinx.coroutines.delay
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupGap
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
    val allItemsProfileSpecific = items.isNotEmpty() && items.all { preference ->
        when (preference) {
            is Preference.PreferenceGroup -> preference.isFullyProfileSpecific()
            is Preference.PreferenceItem<*, *> -> preference.isProfileSpecific
        }
    }
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
        items.fastForEachIndexed { i, preference ->
            when (preference) {
                // Create Preference Group
                is Preference.PreferenceGroup -> {
                    if (!preference.visible) return@fastForEachIndexed
                    val showGroupChip = !allItemsProfileSpecific && preference.isFullyProfileSpecific()

                    item {
                        Column {
                            PreferenceGroupHeader(
                                title = preference.title,
                                isProfileSpecific = showGroupChip,
                                highlighted = preference.title == highlightKey,
                            )
                        }
                    }
                    val positions = rowGroupPositions(preference.preferenceItems)
                    itemsIndexed(preference.preferenceItems) { index, item ->
                        PreferenceItem(
                            item = item,
                            highlightKey = highlightKey,
                            showProfileChip = !allItemsProfileSpecific && !showGroupChip,
                            groupPosition = positions[index],
                        )
                    }
                    item {
                        if (i < items.lastIndex) {
                            RowGroupGap()
                        }
                    }
                }

                // Create Preference Item
                is Preference.PreferenceItem<*, *> -> item {
                    PreferenceItem(
                        item = preference,
                        highlightKey = highlightKey,
                        showProfileChip = !allItemsProfileSpecific,
                        groupPosition = topLevelPositions[i],
                    )
                }
            }
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
