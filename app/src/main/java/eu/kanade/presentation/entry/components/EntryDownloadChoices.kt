package eu.kanade.presentation.entry.components

import mihon.entry.interactions.download.EntryDownloadOptionGroup
import mihon.entry.interactions.download.EntryDownloadOptionSelection
import mihon.entry.interactions.download.EntryDownloadOptions

/**
 * What the download sheet offers and what is picked in it. Groups come in the order they depend on each other: a pick
 * in a group that reloads options lists the groups after it again, and until it has, the sheet shows only the groups up
 * to that one.
 */
data class EntryDownloadChoices(
    val groups: List<EntryDownloadOptionGroup>,
    val selections: Map<String, String?>,
    val reload: Reload? = null,
) {
    /** The groups listed for the current picks: all of them, or those up to the group being listed again. */
    val shownGroups: List<EntryDownloadOptionGroup>
        get() = reload?.let { groups.take(groups.indexOfFirst { group -> group.key == it.groupKey } + 1) } ?: groups

    val canDownload: Boolean
        get() = reload == null &&
            groups.filter(EntryDownloadOptionGroup::required).all { !selections[it.key].isNullOrBlank() }

    val selection: EntryDownloadOptionSelection
        get() = EntryDownloadOptionSelection(selections)

    fun pick(groupKey: String, optionKey: String?): EntryDownloadChoices {
        val index = groups.indexOfFirst { it.key == groupKey }
        if (index < 0) return this
        val picked = selections + (groupKey to optionKey)
        // Picking the group being listed again retries it after a failure.
        val reloads = groups[index].reloadsOptions &&
            (selections[groupKey] != optionKey || reload?.groupKey == groupKey)
        if (!reloads) return copy(selections = picked)
        // The picks after this group were made from its old options, so only the ones up to it are asked for.
        val listedKeys = groups.take(index + 1).map(EntryDownloadOptionGroup::key).toSet()
        return copy(selections = picked, reload = Reload(groupKey, picked.filterKeys(listedKeys::contains)))
    }

    /**
     * The options listed for [reload]. A pick the new options no longer offer falls back to the group's own default,
     * as when the sheet opens, and to the automatic choice when that isn't offered either.
     */
    fun reloaded(options: EntryDownloadOptions): EntryDownloadChoices {
        if (options.listingFailed) return copy(reload = reload?.copy(failed = true))
        return EntryDownloadChoices(
            groups = options.groups,
            selections = options.groups.associate { group ->
                val earlierPick = if (group.key in selections) listOf(selections[group.key]) else emptyList()
                group.key to (earlierPick + group.selectedKey).firstOrNull(group::offers)
            },
        )
    }

    /** [groupKey]'s pick is being listed with [picks], the sheet's picks up to it; [failed] when the source couldn't. */
    data class Reload(
        val groupKey: String,
        val picks: Map<String, String?>,
        val failed: Boolean = false,
    )
}

/** Null stands for the automatic choice, which a required group doesn't have. */
private fun EntryDownloadOptionGroup.offers(key: String?): Boolean {
    return if (key == null) !required else options.any { it.key == key }
}

fun EntryDownloadOptions.toChoices(): EntryDownloadChoices {
    return EntryDownloadChoices(groups = groups, selections = groups.associate { it.key to it.selectedKey })
}
