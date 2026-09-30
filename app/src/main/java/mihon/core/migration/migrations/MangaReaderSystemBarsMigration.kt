package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.feature.profiles.core.ProfileConstants
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.util.lang.withIOContext

/**
 * Converts the manga reader's Fullscreen setting into separate status bar and navigation bar settings.
 *
 * A profile that turned Fullscreen off saw both bars, so it keeps both shown. An enabled or untouched Fullscreen
 * matches the new defaults, which hide both bars.
 */
class MangaReaderSystemBarsMigration : Migration {
    override val version: Float = 68f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val profileStore = migrationContext.get<ProfileStore>() ?: return@withIOContext false
        val profileDatabase = migrationContext.get<ProfileDatabase>() ?: return@withIOContext false
        val profileIds = profileDatabase.getProfiles(includeArchived = true).map { it.id }
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }

        profileIds.forEach { profileId ->
            val store = profileStore.profileStore(profileId)
            val legacyFullscreen = store.getBoolean(LEGACY_FULLSCREEN, true)
            if (legacyFullscreen.isSet()) {
                if (!legacyFullscreen.get()) {
                    store.getBoolean(MangaReaderSettings.SHOW_STATUS_BAR_PREFERENCE_KEY, false).set(true)
                    store.getBoolean(MangaReaderSettings.SHOW_NAVIGATION_BAR_PREFERENCE_KEY, false).set(true)
                }
                legacyFullscreen.delete()
            }
        }

        return@withIOContext true
    }

    private companion object {
        const val LEGACY_FULLSCREEN = "fullscreen"
    }
}
