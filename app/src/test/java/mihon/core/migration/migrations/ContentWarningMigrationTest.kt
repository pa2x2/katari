package mihon.core.migration.migrations

import eu.kanade.domain.source.service.SourcePreferences
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.core.migration.MigrationContext
import mihon.domain.extension.model.ContentWarning
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentWarningMigrationTest {

    // A profile that isn't the active one kept hiding NSFW extensions, and would load them after the update
    @Test
    fun `hidden NSFW extensions stay hidden in every profile`() = runTest {
        val active = MigrationTestPreferenceStore()
        val other = MigrationTestPreferenceStore()
        active.getBoolean(SourcePreferences.LEGACY_SHOW_NSFW_SOURCE_KEY, true).set(true)
        other.getBoolean(SourcePreferences.LEGACY_SHOW_NSFW_SOURCE_KEY, true).set(false)
        val profileDatabase = mockk<ProfileDatabase>()
        coEvery { profileDatabase.getProfiles(includeArchived = true) } returns listOf(profile(1), profile(2))
        val context = MigrationContext(
            dryrun = false,
            previousVersion = 75,
            dependencies = mapOf(
                ProfileStore::class.java to MigrationTestProfileStore(mapOf(1L to active, 2L to other)),
                ProfileDatabase::class.java to profileDatabase,
            ),
        )

        assertTrue(ContentWarningMigration().invoke(context))

        assertEquals(ContentWarning.entries.toSet(), SourcePreferences.enabledContentWarnings(active).get())
        assertEquals(setOf(ContentWarning.SAFE), SourcePreferences.enabledContentWarnings(other).get())
        assertFalse(other.getBoolean(SourcePreferences.LEGACY_SHOW_NSFW_SOURCE_KEY, true).isSet())
    }

    private fun profile(id: Long) = Profile(id, "uuid-$id", "Profile $id", 0, id, false, false)
}
