package mihon.core.migration.migrations

import eu.kanade.domain.source.service.LegacyNsfwSourceConversion
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import mihon.feature.profiles.core.ProfileConstants
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
