package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.getEnumSet
import tachiyomi.core.common.util.lang.withIOContext

class VerticalNavigatorMigration : Migration {
    override val version: Float = 91f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val profileStore = migrationContext.get<ProfileStore>() ?: return@withIOContext false
        val profileDatabase = migrationContext.get<ProfileDatabase>() ?: return@withIOContext false
        val profileIds = profileDatabase.getProfiles(includeArchived = true).map { it.id }

        profileIds.forEach { profileId ->
            val store = profileStore.profileStore(profileId)
            val oldVerticalNavigator = store.getBoolean("pref_webtoon_vertical_navigator", true)
            if (oldVerticalNavigator.get()) {
                store.getEnumSet(
                    MangaReaderSettings.VERTICAL_NAVIGATOR_PREFERENCE_KEY,
                    emptySet<ReadingMode>(),
                ).set(setOf(ReadingMode.WEBTOON, ReadingMode.CONTINUOUS_VERTICAL))
            }
            if (oldVerticalNavigator.isSet()) oldVerticalNavigator.delete()

            val oldVerticalNavigatorOnLeft = store.getBoolean("pref_webtoon_vertical_navigator_on_left", false)
            if (oldVerticalNavigatorOnLeft.isSet()) {
                store.getBoolean(
                    MangaReaderSettings.VERTICAL_NAVIGATOR_ON_LEFT_PREFERENCE_KEY,
                    false,
                ).set(oldVerticalNavigatorOnLeft.get())
                oldVerticalNavigatorOnLeft.delete()
            }
        }

        return@withIOContext true
    }
}
