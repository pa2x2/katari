package eu.kanade.presentation.updates

import kotlinx.datetime.LocalDate

sealed interface UpdatesUiModel<out T> {
    data class Header(val date: LocalDate) : UpdatesUiModel<Nothing>

    data class Item<T>(val item: T) : UpdatesUiModel<T>

    /**
     * Updates of one entry on one day, shown as a single row; when [expanded], its items follow it.
     *
     * @param key stays the same across reloads, so the group keeps its place and expansion.
     */
    data class Group<T>(val key: String, val items: List<T>, val expanded: Boolean) : UpdatesUiModel<T>
}

/** An entry with at least this many updates on one day gets one row for them all. */
const val UPDATES_GROUP_MIN_SIZE = 3

/**
 * Splits [this], newest first, into days, and within a day gathers an entry's updates into a group in place of its
 * newest one once there are [UPDATES_GROUP_MIN_SIZE] of them.
 *
 * @param expandedGroups keys of the groups whose items are shown.
 */
fun <T> List<T>.toUpdatesUiModels(
    dateProvider: (T) -> LocalDate,
    entryIdProvider: (T) -> Long,
    expandedGroups: Set<String>,
): List<UpdatesUiModel<T>> {
    return groupConsecutiveBy(dateProvider)
        .flatMap { (date, items) ->
            val byEntry = items.groupBy(entryIdProvider)
            val placed = mutableSetOf<Long>()
            val dayModels = items.flatMap { item ->
                val entryId = entryIdProvider(item)
                val entryItems = byEntry.getValue(entryId)
                when {
                    entryItems.size < UPDATES_GROUP_MIN_SIZE -> listOf(UpdatesUiModel.Item(item))
                    !placed.add(entryId) -> emptyList()
                    else -> {
                        val key = "$date-$entryId"
                        val expanded = key in expandedGroups
                        listOf(UpdatesUiModel.Group(key, entryItems, expanded)) +
                            if (expanded) entryItems.map { UpdatesUiModel.Item(it) } else emptyList()
                    }
                }
            }
            listOf(UpdatesUiModel.Header(date)) + dayModels
        }
}

private fun <T, K> List<T>.groupConsecutiveBy(keySelector: (T) -> K): List<Pair<K, List<T>>> {
    val groups = mutableListOf<Pair<K, MutableList<T>>>()
    for (element in this) {
        val key = keySelector(element)
        val last = groups.lastOrNull()
        if (last != null && last.first == key) {
            last.second.add(element)
        } else {
            groups.add(key to mutableListOf(element))
        }
    }
    return groups
}
