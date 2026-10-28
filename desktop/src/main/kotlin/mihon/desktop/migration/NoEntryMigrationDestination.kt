package mihon.desktop.migration

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.feature.migration.EntryMigrationDestination
import tachiyomi.domain.entry.model.Entry

/**
 * Desktop can't migrate entries yet, so the library selection, the library update report and the entry screen don't
 * offer it. Stands in until desktop hosts the migration screens.
 */
@Inject
class NoEntryMigrationDestination : EntryMigrationDestination {
    override val isAvailable: Boolean = false

    override fun screen(subjects: Collection<EntryMigrationSubject>): Screen =
        error("Migration is not available on desktop")

    @Composable
    override fun MigrateDialog(
        current: Entry,
        target: Entry,
        onClickTitle: () -> Unit,
        onDismissRequest: () -> Unit,
        onComplete: () -> Unit,
    ) {
        error("Migration is not available on desktop")
    }
}
