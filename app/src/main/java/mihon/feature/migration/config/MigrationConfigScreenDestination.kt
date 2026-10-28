package mihon.feature.migration.config

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.feature.migration.EntryMigrationDestination

@Inject
class MigrationConfigScreenDestination : EntryMigrationDestination {
    override val isAvailable: Boolean = true

    override fun screen(subjects: Collection<EntryMigrationSubject>): Screen = MigrationConfigScreen(subjects)
}
