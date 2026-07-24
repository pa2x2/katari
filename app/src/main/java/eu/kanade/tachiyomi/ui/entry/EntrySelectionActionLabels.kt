package eu.kanade.tachiyomi.ui.entry

import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.selectionEntryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryTypePresentationFeature
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class EntrySelectionActionLabels(
    val markAsReadLabel: StringResource,
    val markAsUnreadLabel: StringResource,
)

fun Iterable<EntryType>.entrySelectionActionLabels(
    presentationFeature: EntryTypePresentationFeature = Injekt.get(),
): EntrySelectionActionLabels {
    val presentation = selectionEntryTypePresentation(presentationFeature)
    return EntrySelectionActionLabels(
        markAsReadLabel = presentation.markAsConsumedLabel,
        markAsUnreadLabel = presentation.markAsUnconsumedLabel,
    )
}
