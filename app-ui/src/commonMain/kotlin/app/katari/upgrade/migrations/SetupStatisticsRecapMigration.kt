package app.katari.upgrade.migrations

import app.katari.upgrade.Migration
import app.katari.upgrade.MigrationContext

class SetupStatisticsRecapMigration : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        migrationContext.statisticsRecapSchedule.setupTask()
        return true
    }
}
