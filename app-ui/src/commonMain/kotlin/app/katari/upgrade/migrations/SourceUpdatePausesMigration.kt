package app.katari.upgrade.migrations

import app.katari.library.update.legacy.LegacyExcludedSourcesConversion
import app.katari.profiles.core.ProfileConstants
import app.katari.upgrade.Migration
import app.katari.upgrade.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

/** Keeps every source that was switched off for library updates paused until resumed, now that pauses can end. */
class SourceUpdatePausesMigration : Migration {
    override val version: Float = 72f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val profileStore = migrationContext.profileStore
        val profileDatabase = migrationContext.profileDatabase
        val profileIds = profileDatabase.getProfiles(includeArchived = true).map { it.id }
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }

        profileIds.forEach { profileId ->
            LegacyExcludedSourcesConversion.convert(profileStore.profileStore(profileId))
        }

        return@withIOContext true
    }
}
