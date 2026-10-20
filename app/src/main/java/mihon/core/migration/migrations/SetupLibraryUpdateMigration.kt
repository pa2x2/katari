package mihon.core.migration.migrations

import eu.kanade.tachiyomi.data.library.LibraryUpdateSchedule
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

class SetupLibraryUpdateMigration : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.application
        LibraryUpdateSchedule.scheduleAll(context)
        return true
    }
}
