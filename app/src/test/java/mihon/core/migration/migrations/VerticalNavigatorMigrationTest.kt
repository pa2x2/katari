package mihon.core.migration.migrations

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.core.migration.MigrationContext
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.getEnumSet

class VerticalNavigatorMigrationTest {

    @Test
    fun `migrates navigator preferences for every profile`() = runTest {
        val first = MigrationTestPreferenceStore()
        val second = MigrationTestPreferenceStore()
        first.getBoolean(OLD_VERTICAL_NAVIGATOR, true).set(false)
        first.getBoolean(OLD_VERTICAL_NAVIGATOR_ON_LEFT, false).set(true)
        second.getBoolean(OLD_VERTICAL_NAVIGATOR, true).set(true)
        val profileStore = MigrationTestProfileStore(mapOf(1L to first, 2L to second))
        val profileDatabase = mockk<ProfileDatabase>()
        coEvery { profileDatabase.getProfiles(includeArchived = true) } returns listOf(profile(1), profile(2))
        val context = MigrationContext(
            dryrun = false,
            previousVersion = 90,
            dependencies = mapOf(
                ProfileStore::class.java to profileStore,
                ProfileDatabase::class.java to profileDatabase,
            ),
        )

        assertTrue(VerticalNavigatorMigration().invoke(context))

        assertEquals(emptySet<ReadingMode>(), first.verticalNavigator().get())
        assertTrue(first.verticalNavigatorOnLeft().get())
        assertEquals(
            setOf(ReadingMode.WEBTOON, ReadingMode.CONTINUOUS_VERTICAL),
            second.verticalNavigator().get(),
        )
        assertFalse(first.getBoolean(OLD_VERTICAL_NAVIGATOR, true).isSet())
        assertFalse(first.getBoolean(OLD_VERTICAL_NAVIGATOR_ON_LEFT, false).isSet())
        assertFalse(second.getBoolean(OLD_VERTICAL_NAVIGATOR, true).isSet())
    }

    @Test
    fun `migration is assigned to released fork upgrade version`() {
        assertEquals(91f, VerticalNavigatorMigration().version)
    }

    private fun profile(id: Long) = Profile(id, "uuid-$id", "Profile $id", 0, id, false, false)

    private fun MigrationTestPreferenceStore.verticalNavigator() = getEnumSet(
        MangaReaderSettings.VERTICAL_NAVIGATOR_PREFERENCE_KEY,
        emptySet<ReadingMode>(),
    )

    private fun MigrationTestPreferenceStore.verticalNavigatorOnLeft() = getBoolean(
        MangaReaderSettings.VERTICAL_NAVIGATOR_ON_LEFT_PREFERENCE_KEY,
        false,
    )

    private companion object {
        const val OLD_VERTICAL_NAVIGATOR = "pref_webtoon_vertical_navigator"
        const val OLD_VERTICAL_NAVIGATOR_ON_LEFT = "pref_webtoon_vertical_navigator_on_left"
    }
}
