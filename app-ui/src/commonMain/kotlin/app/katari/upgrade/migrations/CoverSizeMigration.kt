package app.katari.upgrade.migrations

import app.katari.library.display.legacy.LegacyItemsPerRowConversion
import app.katari.profiles.core.ProfileConstants
import app.katari.upgrade.Migration
import app.katari.upgrade.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

class CoverSizeMigration : Migration {
    override val version: Float = 87f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val profileStore = migrationContext.profileStore
        val profileDatabase = migrationContext.profileDatabase
        val profileIds = profileDatabase.getProfiles(includeArchived = true).map { it.id }
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }

        profileIds.forEach { profileId ->
            LegacyItemsPerRowConversion.convert(profileStore.profileStore(profileId))
        }

        return@withIOContext true
    }
}
