package eu.kanade.tachiyomi.ui.entry

import app.katari.entry.interactions.catalogue.EntryCatalogueFeature
import app.katari.entry.interactions.child.EntryRelatedEntriesFeature
import app.katari.entry.interactions.download.EntryDownloadActionFeature
import app.katari.entry.interactions.navigation.EntryOpenFeature
import app.katari.entry.interactions.source.EntryWebViewFeature
import app.katari.entry.interactions.state.EntryBookmarkFeature
import app.katari.entry.interactions.state.EntryConsumptionFeature
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.util.SourceErrorMessages
import eu.kanade.tachiyomi.ui.entry.updates.EntryUpdateIntervalAvailability

/** Graph services the entry screen reads outside a view model. */
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
