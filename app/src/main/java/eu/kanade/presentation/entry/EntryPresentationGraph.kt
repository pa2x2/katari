package eu.kanade.presentation.entry

import eu.kanade.domain.ui.UiPreferences
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager

/** Graph entry points of the entry composables, including the type vocabulary every surface reads. */
interface EntryPresentationGraph {
    val duplicatePreferences: DuplicatePreferences
    val entryCatalogueFeature: EntryCatalogueFeature
    val entryMigrationFeature: EntryMigrationFeature
    val entryTypePresentationFeature: EntryTypePresentationFeature
    val libraryPreferences: LibraryPreferences
    val sourceManager: SourceManager
    val uiPreferences: UiPreferences
}
