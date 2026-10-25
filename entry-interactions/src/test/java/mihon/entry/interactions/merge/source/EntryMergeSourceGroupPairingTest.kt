package mihon.entry.interactions.merge.source

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMergeSourceGroupPairingTest {
    @Test
    fun `members pair by position when both sources list as many and by title otherwise`() {
        val current = listOf(member(1, "Season 1"), member(2, "Season 2"))

        pairSourceGroupMembers(current, listOf(member(11, "First Season"), member(12, "Second Season")))
            .map { (old, new) -> old.id to new.id } shouldBe listOf(1L to 11L, 2L to 12L)
        pairSourceGroupMembers(current, listOf(member(12, "season 2 "), member(13, "Movie"), member(14, "OVA")))
            .map { (old, new) -> old.id to new.id } shouldBe listOf(2L to 12L)
    }

    private fun member(id: Long, title: String) = Entry.create().copy(id = id, title = title)
}
