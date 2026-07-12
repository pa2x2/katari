package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import mihon.entry.interactions.reader.settings.ReaderPreferences
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.util.lang.withIOContext

class VerticalNavigatorMigration : Migration {
    override val version: Float = 91f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val profileStore = migrationContext.get<ProfileStore>() ?: return@withIOContext false
        val profileDatabase = migrationContext.get<ProfileDatabase>() ?: return@withIOContext false
        val profileIds = profileDatabase.getProfiles(includeArchived = true).map { it.id }

        profileIds.forEach { profileId ->
            val store = profileStore.profileStore(profileId)
            val readerPreferences = ReaderPreferences(store)
            val oldVerticalNavigator = store.getBoolean("pref_webtoon_vertical_navigator", true)
            if (oldVerticalNavigator.get()) {
                readerPreferences.verticalNavigator.set(setOf(ReadingMode.WEBTOON, ReadingMode.CONTINUOUS_VERTICAL))
            }
            if (oldVerticalNavigator.isSet()) oldVerticalNavigator.delete()

            val oldVerticalNavigatorOnLeft = store.getBoolean("pref_webtoon_vertical_navigator_on_left", false)
            if (oldVerticalNavigatorOnLeft.isSet()) {
                readerPreferences.verticalNavigatorOnLeft.set(oldVerticalNavigatorOnLeft.get())
                oldVerticalNavigatorOnLeft.delete()
            }
        }

        return@withIOContext true
    }
}
