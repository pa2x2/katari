package eu.kanade.tachiyomi.ui.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.selectionEntryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.presentation.EntryTypePresentation
import mihon.entry.interactions.presentation.EntryTypePresentationFeature

data class EntrySelectionActionLabels(
    val markAsReadLabel: StringResource,
    val markAsUnreadLabel: StringResource,
)

@Composable
@ReadOnlyComposable
fun Iterable<EntryType>.entrySelectionActionLabels(): EntrySelectionActionLabels {
    return selectionEntryTypePresentation().toActionLabels()
}

fun Iterable<EntryType>.entrySelectionActionLabels(
    presentationFeature: EntryTypePresentationFeature,
): EntrySelectionActionLabels {
    return selectionEntryTypePresentation(presentationFeature).toActionLabels()
}

private fun EntryTypePresentation.toActionLabels() = EntrySelectionActionLabels(
    markAsReadLabel = markAsConsumedLabel,
    markAsUnreadLabel = markAsUnconsumedLabel,
)
