package mihon.entry.interactions.host

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DuplicateCandidateTrackMatchesTest {

    @Test
    fun `duplicates require the same service and remote identity`() {
        val matches = duplicateCandidateTrackEntryIds(
            entryId = 1,
            libraryMemberIds = setOf(2, 3, 4, 5),
            tracks = listOf(
                DuplicateCandidateTrackKey(entryId = 1, trackerId = 7, remoteId = 70),
                DuplicateCandidateTrackKey(entryId = 1, trackerId = 8, remoteId = 0),
                DuplicateCandidateTrackKey(entryId = 2, trackerId = 7, remoteId = 70),
                DuplicateCandidateTrackKey(entryId = 3, trackerId = 9, remoteId = 70),
                DuplicateCandidateTrackKey(entryId = 4, trackerId = 7, remoteId = 71),
                DuplicateCandidateTrackKey(entryId = 5, trackerId = 8, remoteId = 0),
                DuplicateCandidateTrackKey(entryId = 6, trackerId = 7, remoteId = 70),
            ),
        )

        matches shouldBe setOf(2L, 5L)
    }
}
