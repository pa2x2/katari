package tachiyomi.domain.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MergeGroupReplacementTest {

    @Test
    fun `replaces non target member and keeps current target`() {
        val replacement = replaceMergeGroupMember(
            currentId = 2L,
            replacementId = 4L,
            currentGroup = group(targetId = 1L, memberIds = listOf(1L, 2L, 3L)),
            replacementGroup = emptyList(),
        )

        replacement shouldBe MergeGroupReplacement.Upsert(
            targetId = 1L,
            orderedMemberIds = listOf(1L, 4L, 3L),
            targetIdsToRemoveReplacementFrom = emptySet(),
        )
    }

    @Test
    fun `replaces target member and promotes replacement as target`() {
        val replacement = replaceMergeGroupMember(
            currentId = 1L,
            replacementId = 4L,
            currentGroup = group(targetId = 1L, memberIds = listOf(1L, 2L, 3L)),
            replacementGroup = emptyList(),
        )

        replacement shouldBe MergeGroupReplacement.Upsert(
            targetId = 4L,
            orderedMemberIds = listOf(4L, 2L, 3L),
            targetIdsToRemoveReplacementFrom = emptySet(),
        )
    }

    @Test
    fun `deduplicates when replacement already belongs to same group`() {
        val currentGroup = group(targetId = 1L, memberIds = listOf(1L, 2L, 4L))

        val replacement = replaceMergeGroupMember(
            currentId = 2L,
            replacementId = 4L,
            currentGroup = currentGroup,
            replacementGroup = currentGroup,
        )

        replacement shouldBe MergeGroupReplacement.Upsert(
            targetId = 1L,
            orderedMemberIds = listOf(1L, 4L),
            targetIdsToRemoveReplacementFrom = emptySet(),
        )
    }

    @Test
    fun `marks replacement old group for cleanup`() {
        val replacement = replaceMergeGroupMember(
            currentId = 2L,
            replacementId = 4L,
            currentGroup = group(targetId = 1L, memberIds = listOf(1L, 2L, 3L)),
            replacementGroup = group(targetId = 4L, memberIds = listOf(4L, 5L)),
        )

        replacement shouldBe MergeGroupReplacement.Upsert(
            targetId = 1L,
            orderedMemberIds = listOf(1L, 4L, 3L),
            targetIdsToRemoveReplacementFrom = setOf(4L),
        )
    }

    @Test
    fun `deletes group when replacement collapses group to one member`() {
        val replacement = replaceMergeGroupMember(
            currentId = 1L,
            replacementId = 2L,
            currentGroup = group(targetId = 1L, memberIds = listOf(1L, 2L)),
            replacementGroup = group(targetId = 1L, memberIds = listOf(1L, 2L)),
        )

        replacement shouldBe MergeGroupReplacement.Delete(
            targetId = 1L,
            targetIdsToRemoveReplacementFrom = emptySet(),
        )
    }

    private fun group(targetId: Long, memberIds: List<Long>): List<MergeGroupMember> {
        return memberIds.mapIndexed { index, memberId ->
            MergeGroupMember(
                targetId = targetId,
                memberId = memberId,
                position = index.toLong(),
            )
        }
    }
}
