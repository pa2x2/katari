package mihon.core.migration.migrations

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.core.migration.MigrationContext
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.getEnum

class ChapterTransitionMigrationTest {

    @Test
    fun `migrates exactly the registered profiles`() = runTest {
        val first = MigrationTestPreferenceStore()
        first.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true).set(true)
        val second = MigrationTestPreferenceStore()
        second.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true).set(false)
        val unregistered = MigrationTestPreferenceStore()
        unregistered.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true).set(false)

        assertTrue(
            invokeMigration(
                stores = mapOf(1L to unregistered, 2L to first, 3L to second),
                registeredProfiles = listOf(profile(2), profile(3)),
            ),
        )

        assertEquals(ChapterTransitionMode.ALWAYS, first.chapterTransitionMode().get())
        assertEquals(ChapterTransitionMode.WHEN_NEEDED, second.chapterTransitionMode().get())
        assertFalse(unregistered.chapterTransitionMode().isSet())
    }

    @Test
    fun `falls back to the default profile store when no profiles exist`() = runTest {
        val onlyDefault = MigrationTestPreferenceStore()
        onlyDefault.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true).set(false)

        assertTrue(invokeMigration(mapOf(1L to onlyDefault)))

        assertEquals(ChapterTransitionMode.WHEN_NEEDED, onlyDefault.chapterTransitionMode().get())
        assertFalse(onlyDefault.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true).isSet())
    }

    private suspend fun invokeMigration(
        stores: Map<Long, MigrationTestPreferenceStore>,
        registeredProfiles: List<Profile> = emptyList(),
    ): Boolean {
        val profileDatabase = mockk<ProfileDatabase>()
        coEvery { profileDatabase.getProfiles(includeArchived = true) } returns registeredProfiles
        val context = MigrationContext(
            dryrun = false,
            previousVersion = 64,
            dependencies = mapOf(
                ProfileStore::class.java to MigrationTestProfileStore(stores),
                ProfileDatabase::class.java to profileDatabase,
            ),
        )
        return ChapterTransitionMigration().invoke(context)
    }

    private fun profile(id: Long) = Profile(id, "uuid-$id", "Profile $id", 0, id, false, false)

    private fun MigrationTestPreferenceStore.chapterTransitionMode() = getEnum(
        MangaReaderSettings.CHAPTER_TRANSITION_PREFERENCE_KEY,
        ChapterTransitionMode.ALWAYS,
    )

    private companion object {
        const val LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION = "always_show_chapter_transition"
    }
}
