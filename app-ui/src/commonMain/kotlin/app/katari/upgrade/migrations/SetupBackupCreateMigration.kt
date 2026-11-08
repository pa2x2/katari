package app.katari.upgrade.migrations

import app.katari.upgrade.Migration
import app.katari.upgrade.MigrationContext

class SetupBackupCreateMigration : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        migrationContext.backupCreateSchedule.setupTask()
        return true
    }
}
