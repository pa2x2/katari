package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import mihon.feature.library.update.legacy.LegacyExcludedSourcesConversion
import mihon.feature.profiles.core.ProfileConstants
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.util.lang.withIOContext

/** Keeps every source that was switched off for library updates paused until resumed, now that pauses can end. */
class SourceUpdatePausesMigration : Migration {
    override val version: Float = 72f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val profileStore = migrationContext.get<ProfileStore>() ?: return@withIOContext false
        val profileDatabase = migrationContext.get<ProfileDatabase>() ?: return@withIOContext false
        val profileIds = profileDatabase.getProfiles(includeArchived = true).map { it.id }
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }

        profileIds.forEach { profileId ->
            LegacyExcludedSourcesConversion.convert(profileStore.profileStore(profileId))
        }

        return@withIOContext true
    }
}
