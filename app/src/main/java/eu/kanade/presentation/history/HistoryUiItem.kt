package eu.kanade.presentation.history

import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.history.model.HistoryItem

/**
 * UI item for the unified History list. Holds the domain [HistoryItem] plus the resolved
 * visible entry metadata (merged target id/title/cover).
 */
data class HistoryUiItem(
    val historyItem: HistoryItem,
    val visibleEntryId: Long,
    val visibleTitle: String,
    val visibleCoverData: EntryCover,
)
