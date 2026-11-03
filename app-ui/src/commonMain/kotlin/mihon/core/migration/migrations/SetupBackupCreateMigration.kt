package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

class SetupBackupCreateMigration : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        migrationContext.backupCreateSchedule.setupTask()
        return true
    }
}
