package app.katari.upgrade.migrations

import app.katari.profiles.core.ProfileConstants
import app.katari.upgrade.Migration
import app.katari.upgrade.MigrationContext
import eu.kanade.domain.source.service.LegacyNsfwSourceConversion
import tachiyomi.core.common.util.lang.withIOContext

class ContentWarningMigration : Migration {
    override val version: Float = 76f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val profileStore = migrationContext.profileStore
        val profileDatabase = migrationContext.profileDatabase
        val profileIds = profileDatabase.getProfiles(includeArchived = true).map { it.id }
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }

        profileIds.forEach { profileId ->
            LegacyNsfwSourceConversion.convert(profileStore.profileStore(profileId))
        }

        return@withIOContext true
    }
}
