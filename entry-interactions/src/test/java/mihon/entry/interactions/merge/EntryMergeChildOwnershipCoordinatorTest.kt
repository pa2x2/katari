package mihon.entry.interactions.merge

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.merge.host.EntryMergeMembershipSnapshot
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMergeChildOwnershipCoordinatorTest {
    @Test
    fun `batch ownership retains members outside the supplied Library population`() = runTest {
        val libraryEntry = entry(id = 1L, favorite = true)
        val nonLibraryEntry = entry(id = 2L, favorite = false)
        val membership = EntryMergeMembershipSnapshot(
            profileId = PROFILE_ID,
            targetEntryId = libraryEntry.id,
            orderedEntryIds = listOf(libraryEntry.id, nonLibraryEntry.id),
        )
        val coordinator = EntryMergeChildOwnershipCoordinator(
            RecordingEntryMergeHost(listOf(libraryEntry, nonLibraryEntry), listOf(membership)),
        )

        val resolution = coordinator.resolveChildOwnership(PROFILE_ID, setOf(libraryEntry.id)).getValue(libraryEntry.id)

        resolution.orderedOwners shouldBe listOf(libraryEntry, nonLibraryEntry)
        resolution.visibleEntryId shouldBe libraryEntry.id
    }

    private fun entry(id: Long, favorite: Boolean): Entry {
        return Entry.create().copy(
            id = id,
            profileId = PROFILE_ID,
            favorite = favorite,
            type = EntryType.BOOK,
            title = "Entry $id",
        )
    }

    private companion object {
        const val PROFILE_ID = 7L
    }
}
