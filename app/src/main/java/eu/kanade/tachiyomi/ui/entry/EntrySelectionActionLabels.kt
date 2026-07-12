package eu.kanade.tachiyomi.ui.entry

import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.selectionEntryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType

data class EntrySelectionActionLabels(
    val markAsReadLabel: StringResource,
    val markAsUnreadLabel: StringResource,
)

fun Iterable<EntryType>.entrySelectionActionLabels(): EntrySelectionActionLabels {
    val presentation = selectionEntryTypePresentation()
    return EntrySelectionActionLabels(
        markAsReadLabel = presentation.markAsConsumedLabel,
        markAsUnreadLabel = presentation.markAsUnconsumedLabel,
    )
}
