package mihon.feature.migration

import androidx.compose.runtime.Composable
import eu.kanade.presentation.util.Screen
import mihon.entry.interactions.migration.EntryMigrationSubject
import tachiyomi.domain.entry.model.Entry

/** Where shared screens, such as the library's selection, start migrating entries to another source. */
interface EntryMigrationDestination {
    /** False where entries can't be migrated yet; what would start a migration is then hidden. */
    val isAvailable: Boolean

    /** Only called while [isAvailable]. */
    fun screen(subjects: Collection<EntryMigrationSubject>): Screen

    /**
     * Asks what to carry over from [current] to [target] and migrates it, such as from the entry screen's duplicate
     * sheet; its Show entry action calls [onClickTitle]. Only shown while [isAvailable].
     */
    @Composable
    fun MigrateDialog(current: Entry, target: Entry, onClickTitle: () -> Unit, onDismissRequest: () -> Unit)
}
