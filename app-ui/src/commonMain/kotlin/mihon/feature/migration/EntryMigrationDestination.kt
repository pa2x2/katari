package mihon.feature.migration

import eu.kanade.presentation.util.Screen
import mihon.entry.interactions.migration.EntryMigrationSubject

/** Where shared screens, such as the library's selection, start migrating entries to another source. */
interface EntryMigrationDestination {
    /** False where entries can't be migrated yet; what would start a migration is then hidden. */
    val isAvailable: Boolean

    /** Only called while [isAvailable]. */
    fun screen(subjects: Collection<EntryMigrationSubject>): Screen
}
