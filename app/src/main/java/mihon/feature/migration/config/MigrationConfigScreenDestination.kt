package mihon.feature.migration.config

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.feature.migration.EntryMigrationDestination
import mihon.feature.migration.dialog.MigrateEntryDialog
import tachiyomi.domain.entry.model.Entry

@Inject
class MigrationConfigScreenDestination : EntryMigrationDestination {
    override val isAvailable: Boolean = true

    override fun screen(subjects: Collection<EntryMigrationSubject>): Screen = MigrationConfigScreen(subjects)

    @Composable
    override fun MigrateDialog(
        current: Entry,
        target: Entry,
        onClickTitle: () -> Unit,
        onDismissRequest: () -> Unit,
        onComplete: () -> Unit,
    ) {
        MigrateEntryDialog(
            current = current,
            target = target,
            onClickTitle = onClickTitle,
            onDismissRequest = onDismissRequest,
            onComplete = onComplete,
        )
    }
}
