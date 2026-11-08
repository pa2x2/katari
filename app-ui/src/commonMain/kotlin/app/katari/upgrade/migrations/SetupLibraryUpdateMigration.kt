package app.katari.upgrade.migrations

import app.katari.upgrade.Migration
import app.katari.upgrade.MigrationContext

class SetupLibraryUpdateMigration : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        migrationContext.libraryUpdateSchedule.scheduleAll()
        return true
    }
}
