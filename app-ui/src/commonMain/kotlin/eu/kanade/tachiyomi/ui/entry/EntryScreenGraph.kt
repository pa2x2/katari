package eu.kanade.tachiyomi.ui.entry

import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.util.SourceErrorMessages
import eu.kanade.tachiyomi.ui.entry.updates.EntryUpdateIntervalAvailability
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.child.EntryRelatedEntriesFeature
import mihon.entry.interactions.download.EntryDownloadActionFeature
import mihon.entry.interactions.navigation.EntryOpenFeature
import mihon.entry.interactions.source.EntryWebViewFeature
import mihon.entry.interactions.state.EntryBookmarkFeature
import mihon.entry.interactions.state.EntryConsumptionFeature

/** Graph services the entry screen and its tracking dialog read outside a view model. */
interface EntryScreenGraph {
    val entryBookmarkFeature: EntryBookmarkFeature
    val entryCatalogueFeature: EntryCatalogueFeature
    val entryConsumptionFeature: EntryConsumptionFeature
    val entryDownloadActionFeature: EntryDownloadActionFeature
    val entryOpenFeature: EntryOpenFeature
    val entryRelatedEntriesFeature: EntryRelatedEntriesFeature
    val entryWebViewFeature: EntryWebViewFeature
    val uiPreferences: UiPreferences
    val sourceErrorMessages: SourceErrorMessages
    val entryUpdateIntervalAvailability: EntryUpdateIntervalAvailability
}
