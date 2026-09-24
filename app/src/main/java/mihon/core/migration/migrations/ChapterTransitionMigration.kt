package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.feature.profiles.core.ProfileConstants
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.core.common.util.lang.withIOContext

/**
 * Converts the boolean "always show chapter transition" setting into the tri-state chapter
 * transition display mode.
 *
 * Users who explicitly disabled the boolean only ever meant "stop showing the card between
 * seamless chapters", which maps to [ChapterTransitionMode.WHEN_NEEDED]; an explicitly enabled
 * boolean maps to [ChapterTransitionMode.ALWAYS]. Untouched installs stay unset and keep the
 * provider default ([ChapterTransitionMode.ALWAYS]).
 */
class ChapterTransitionMigration : Migration {
    override val version: Float = 65f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val profileStore = migrationContext.get<ProfileStore>() ?: return@withIOContext false
        val profileDatabase = migrationContext.get<ProfileDatabase>() ?: return@withIOContext false
        val profileIds = profileDatabase.getProfiles(includeArchived = true).map { it.id }
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }

        profileIds.forEach { profileId ->
            val store = profileStore.profileStore(profileId)
            val oldAlwaysShowTransition = store.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true)
            if (oldAlwaysShowTransition.isSet()) {
                store.getEnum(
                    MangaReaderSettings.CHAPTER_TRANSITION_PREFERENCE_KEY,
                    ChapterTransitionMode.ALWAYS,
                ).set(
                    if (oldAlwaysShowTransition.get()) {
                        ChapterTransitionMode.ALWAYS
                    } else {
                        ChapterTransitionMode.WHEN_NEEDED
                    },
                )
                oldAlwaysShowTransition.delete()
            }
        }

        return@withIOContext true
    }

    private companion object {
        const val LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION = "always_show_chapter_transition"
    }
}
