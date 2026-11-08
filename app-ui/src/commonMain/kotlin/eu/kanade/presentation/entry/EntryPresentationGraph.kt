package eu.kanade.presentation.entry

import app.katari.entry.interactions.catalogue.EntryCatalogueFeature
import app.katari.entry.interactions.migration.EntryMigrationFeature
import app.katari.entry.interactions.presentation.EntryTypePresentationFeature
import eu.kanade.domain.ui.UiPreferences
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
