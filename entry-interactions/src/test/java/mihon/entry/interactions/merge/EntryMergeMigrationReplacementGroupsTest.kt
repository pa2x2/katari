package mihon.entry.interactions.merge

import io.kotest.matchers.collections.shouldContainExactly
import mihon.entry.interactions.merge.host.EntryMergeMembershipSnapshot
import org.junit.jupiter.api.Test

class EntryMergeMigrationReplacementGroupsTest {
    @Test
    fun `replacement transfers one member and dissolves a depleted source group`() {
        val replacements = replacementGroups(
            currentEntryId = 2L,
            replacementEntryId = 3L,
            currentGroup = EntryMergeMembershipSnapshot(7L, 1L, listOf(1L, 2L)),
            replacementGroup = EntryMergeMembershipSnapshot(7L, 3L, listOf(3L, 4L)),
        )

        replacements shouldContainExactly listOf(
            EntryMergeMembershipSnapshot(7L, 1L, listOf(1L, 3L)),
        )
    }
}
