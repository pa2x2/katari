package mihon.desktop.migration

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.feature.migration.EntryMigrationDestination

/**
 * Desktop can't migrate entries yet, so the library selection and the library update report don't offer it. Stands in
 * until desktop hosts the migration screens.
 */
@Inject
class NoEntryMigrationDestination : EntryMigrationDestination {
    override val isAvailable: Boolean = false

    override fun screen(subjects: Collection<EntryMigrationSubject>): Screen =
        error("Migration is not available on desktop")
}
