package tachiyomi.domain.updates.model

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.util.applyFilter

/** What the user narrowed the Updates feed to. */
data class UpdatesFeedFilter(
    val downloaded: TriState = TriState.DISABLED,
    val unseen: TriState = TriState.DISABLED,
    val started: TriState = TriState.DISABLED,
    val bookmarked: TriState = TriState.DISABLED,
    val categories: Selection<Long> = Selection(),
    val types: Selection<EntryType> = Selection(),
    val sources: Selection<Long> = Selection(),
) {
    val isActive: Boolean
        get() = listOf(downloaded, unseen, started, bookmarked).any { it != TriState.DISABLED } ||
            categories.isActive ||
            types.isActive ||
            sources.isActive

    /** @param isDownloaded asked last and only when the downloaded filter is on, since it looks at storage. */
    fun matches(row: UpdatesFeedRow, isDownloaded: () -> Boolean): Boolean {
        val update = row.update
        return applyFilter(unseen) { !update.read } &&
            applyFilter(started) { update.started } &&
            applyFilter(bookmarked) { update.bookmark } &&
            categories.matches(row.categoryIds) &&
            types.matches(setOf(update.entryType)) &&
            sources.matches(setOf(update.sourceId)) &&
            applyFilter(downloaded, isDownloaded)
    }

    /** Values marked with a tri-state: an update needs one of the [included] ones and none of the [excluded] ones. */
    data class Selection<T>(
        val included: Set<T> = emptySet(),
        val excluded: Set<T> = emptySet(),
    ) {
        val isActive: Boolean
            get() = included.isNotEmpty() || excluded.isNotEmpty()

        val all: Set<T>
            get() = included + excluded

        fun stateOf(value: T): TriState = when (value) {
            in included -> TriState.ENABLED_IS
            in excluded -> TriState.ENABLED_NOT
            else -> TriState.DISABLED
        }

        fun matches(values: Set<T>): Boolean {
            return (included.isEmpty() || values.any { it in included }) && values.none { it in excluded }
        }
    }
}
