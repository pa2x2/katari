package mihon.feature.profiles.core

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalFeature
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

class ProfileDeletionTest {

    private val profileId = 2L
    private val entries = listOf(Entry.create().copy(id = 7L, profileId = profileId))

    private val profileDatabase = mockk<ProfileDatabase> {
        coEvery { deleteProfile(profileId) } just runs
    }
    private val profileStore = mockk<ProfileStore> {
        every { deleteProfileState(profileId) } just runs
    }
    private val entryRepository = mockk<EntryRepository> {
        coEvery { getAllEntriesByProfile(profileId) } returns entries
    }
    private val destructiveRemoval = mockk<EntryDestructiveRemovalFeature>()

    private fun deletion() = ProfileDeletion(
        profileDatabase = profileDatabase,
        profileStore = profileStore,
        extensions = mockk(relaxed = true),
        entryRepository = entryRepository,
        destructiveRemoval = destructiveRemoval,
        bookReaderFonts = mockk(relaxed = true),
        libraryUpdateQueue = mockk(relaxed = true),
        libraryUpdateSchedule = mockk(relaxed = true),
    )

    @Test
    fun `deletion preserves profile when destructive removal fails transactionally`() = runTest {
        coEvery { destructiveRemoval.remove(entries) } returns
            EntryDestructiveRemovalResult.Failed(entries, IllegalStateException("removal failed"))

        assertThrows<IllegalStateException> {
            deletion().delete(profileId)
        }

        coVerify(exactly = 0) { profileDatabase.deleteProfile(profileId) }
        verify(exactly = 0) { profileStore.deleteProfileState(profileId) }
    }
}
