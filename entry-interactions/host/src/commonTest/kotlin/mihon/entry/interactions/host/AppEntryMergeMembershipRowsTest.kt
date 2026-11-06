package mihon.entry.interactions.host

import io.kotest.matchers.shouldBe
import mihon.entry.interactions.merge.host.EntryMergeMembershipSnapshot
import org.junit.jupiter.api.Test

class AppEntryMergeMembershipRowsTest {

    @Test
    fun `a merge with one member left, as sync can leave it, reads as no merge instead of failing the library`() {
        // Target 1 merged with 2, and 5 left alone: its other member never synced here
        val rows = listOf(1L to 1L, 1L to 2L, 5L to 5L)

        rows.toMemberships(profileId = 3) shouldBe listOf(EntryMergeMembershipSnapshot(3, 1, listOf(1, 2)))
    }
}
