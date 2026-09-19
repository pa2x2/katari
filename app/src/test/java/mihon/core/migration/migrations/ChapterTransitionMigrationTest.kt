package mihon.core.migration.migrations

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.core.migration.MigrationContext
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.reader.settings.MangaReaderSettingsProvider
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChapterTransitionMigrationTest {

    @Test
    fun `explicitly enabled boolean migrates to always showing transitions`() = runTest {
        val store = MigrationTestPreferenceStore()
        store.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true).set(true)

        assertTrue(invokeMigration(store))

        assertEquals(ChapterTransitionMode.ALWAYS, MangaReaderSettingsProvider(store).chapterTransitionMode.get())
        assertFalse(store.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true).isSet())
    }

    @Test
    fun `explicitly disabled boolean migrates to showing transitions only when needed`() = runTest {
        val store = MigrationTestPreferenceStore()
        store.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true).set(false)

        assertTrue(invokeMigration(store))

        assertEquals(ChapterTransitionMode.WHEN_NEEDED, MangaReaderSettingsProvider(store).chapterTransitionMode.get())
        assertFalse(store.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true).isSet())
    }

    @Test
    fun `untouched boolean leaves the tri-state unset so the provider default applies`() = runTest {
        val store = MigrationTestPreferenceStore()

        assertTrue(invokeMigration(store))

        assertFalse(MangaReaderSettingsProvider(store).chapterTransitionMode.isSet())
        assertEquals(ChapterTransitionMode.ALWAYS, MangaReaderSettingsProvider(store).chapterTransitionMode.get())
    }

    @Test
    fun `migrates exactly the registered profiles`() = runTest {
        val first = MigrationTestPreferenceStore()
        first.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true).set(true)
        val second = MigrationTestPreferenceStore()
        second.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true).set(false)
        val unregistered = MigrationTestPreferenceStore()
        unregistered.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true).set(false)
        val profileStore = MigrationTestProfileStore(mapOf(1L to unregistered, 2L to first, 3L to second))
        val profileDatabase = mockk<ProfileDatabase>()
        coEvery { profileDatabase.getProfiles(includeArchived = true) } returns listOf(profile(2), profile(3))
        val context = MigrationContext(
            dryrun = false,
            previousVersion = 64,
            dependencies = mapOf(
                ProfileStore::class.java to profileStore,
                ProfileDatabase::class.java to profileDatabase,
            ),
        )

        assertTrue(ChapterTransitionMigration().invoke(context))

        assertEquals(ChapterTransitionMode.ALWAYS, MangaReaderSettingsProvider(first).chapterTransitionMode.get())
        assertEquals(
            ChapterTransitionMode.WHEN_NEEDED,
            MangaReaderSettingsProvider(second).chapterTransitionMode.get(),
        )
        assertFalse(MangaReaderSettingsProvider(unregistered).chapterTransitionMode.isSet())
    }

    @Test
    fun `falls back to the default profile store when no profiles exist`() = runTest {
        val onlyDefault = MigrationTestPreferenceStore()
        onlyDefault.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true).set(false)
        val profileStore = MigrationTestProfileStore(mapOf(1L to onlyDefault))
        val profileDatabase = mockk<ProfileDatabase>()
        coEvery { profileDatabase.getProfiles(includeArchived = true) } returns emptyList()
        val context = MigrationContext(
            dryrun = false,
            previousVersion = 64,
            dependencies = mapOf(
                ProfileStore::class.java to profileStore,
                ProfileDatabase::class.java to profileDatabase,
            ),
        )

        assertTrue(ChapterTransitionMigration().invoke(context))

        assertEquals(
            ChapterTransitionMode.WHEN_NEEDED,
            MangaReaderSettingsProvider(onlyDefault).chapterTransitionMode.get(),
        )
        assertFalse(onlyDefault.getBoolean(LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION, true).isSet())
    }

    @Test
    fun `migration is assigned to the next released fork version`() {
        assertEquals(65f, ChapterTransitionMigration().version)
    }

    private suspend fun invokeMigration(store: MigrationTestPreferenceStore): Boolean {
        val profileStore = MigrationTestProfileStore(mapOf(1L to store))
        val profileDatabase = mockk<ProfileDatabase>()
        coEvery { profileDatabase.getProfiles(includeArchived = true) } returns emptyList()
        val context = MigrationContext(
            dryrun = false,
            previousVersion = 64,
            dependencies = mapOf(
                ProfileStore::class.java to profileStore,
                ProfileDatabase::class.java to profileDatabase,
            ),
        )
        return ChapterTransitionMigration().invoke(context)
    }

    private fun profile(id: Long) = Profile(id, "uuid-$id", "Profile $id", 0, id, false, false)

    private companion object {
        const val LEGACY_ALWAYS_SHOW_CHAPTER_TRANSITION = "always_show_chapter_transition"
    }
}
