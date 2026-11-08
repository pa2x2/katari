package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import mihon.feature.profiles.core.ProfileConstants
import tachiyomi.core.common.preference.PlatformPreferenceFile
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.lang.withIOContext

/**
 * Deletes what trackers kept in preferences before Katari dropped them: each profile's tracker sign-ins, tracker
 * settings and library filters, a sign-in left waiting for its browser redirect, and the tracker updates not sent yet.
 * Database migration 75 deletes what they kept in the database.
 */
class TrackerDataCleanupMigration : Migration {
    override val version: Float = 93f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val profileStore = migrationContext.profileStore
        val profileIds = migrationContext.profileDatabase.getProfiles(includeArchived = true).map { it.id }
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }

        profileIds.forEach { profileId ->
            listOf(profileStore.profileStore(profileId), profileStore.privateStore(profileId)).forEach { store ->
                store.deleteKeys { key -> key in PROFILE_KEYS || PROFILE_KEY_PREFIXES.any(key::startsWith) }
            }
        }
        profileStore.basePreferenceStore().deleteKeys { it in BASE_KEYS }
        migrationContext.preferenceFiles.open(PlatformPreferenceFile.LEGACY_TRACKING_QUEUE).deleteKeys { true }

        return@withIOContext true
    }

    private fun PreferenceStore.deleteKeys(matches: (String) -> Boolean) {
        getAll().keys.filter(matches).forEach { key -> getString(key).delete() }
    }

    private companion object {
        val PROFILE_KEYS = setOf(
            "anilist_score_type",
            "kitsu_score_type",
            "mangabaka_score_type",
            "pref_auto_update_manga_sync_key",
            "pref_auto_update_manga_on_mark_read",
        )
        val PROFILE_KEY_PREFIXES = listOf(
            "pref_filter_library_tracked_",
            Preference.privateKey("pref_mangasync_username_"),
            Preference.privateKey("pref_mangasync_displayname_"),
            Preference.privateKey("pref_mangasync_password_"),
            Preference.privateKey("pref_tracker_auth_expired_"),
            Preference.privateKey("track_token_"),
            Preference.privateKey("track_oauth_state_"),
            Preference.privateKey("track_oauth_code_verifier_"),
        )
        val BASE_KEYS = setOf(
            // The score format before each profile kept its own, on installs older than profile preferences
            "anilist_score_type",
            Preference.appStateKey("hikka_oauth_profile_id"),
        )
    }
}
