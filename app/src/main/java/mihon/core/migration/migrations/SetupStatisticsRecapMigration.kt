package mihon.core.migration.migrations

import eu.kanade.tachiyomi.data.statistics.StatisticsRecapNotificationJob
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

class SetupStatisticsRecapMigration : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.application
        StatisticsRecapNotificationJob.setupTask(context)
        return true
    }
}
