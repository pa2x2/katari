package app.katari.upgrade.migrations

import app.katari.profiles.core.ProfileConstants
import app.katari.upgrade.Migration
import app.katari.upgrade.MigrationContext
import eu.kanade.domain.source.service.LegacyMigrationFlagsConversion
import tachiyomi.core.common.util.lang.withIOContext

class MigrationOptionsMigration : Migration {
    override val version: Float = 92f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val profileStore = migrationContext.profileStore
        val profileDatabase = migrationContext.profileDatabase
        val profileIds = profileDatabase.getProfiles(includeArchived = true).map { it.id }
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }

        profileIds.forEach { profileId ->
            LegacyMigrationFlagsConversion.convert(profileStore.profileStore(profileId))
        }

        return@withIOContext true
    }
}
