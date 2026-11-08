package app.katari.upgrade.migrations

import app.katari.core.common.FeatureFlags
import app.katari.upgrade.Migration
import app.katari.upgrade.MigrationContext
import kotlin.uuid.ExperimentalUuidApi

class InstallationIdMigration : Migration {
    override val version: Float = Migration.ALWAYS

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val installationId = migrationContext.installationPreferences.installationId
        if (!installationId.isSet()) installationId.set(FeatureFlags.newInstallationId())
        return true
    }
}
