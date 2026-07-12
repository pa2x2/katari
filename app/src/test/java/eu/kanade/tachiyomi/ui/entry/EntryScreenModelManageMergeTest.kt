package eu.kanade.tachiyomi.ui.entry

import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryScreenModelManageMergeTest {

    @Test
    fun `dialogRemainingIds removes both merge removals and library removals`() {
        val dialog = manageMergeDialog(
            targetId = 1L,
            memberIds = listOf(1L, 2L, 3L, 4L),
        )

        dialogRemainingIds(dialog, entryIdsToRemove = listOf(3L, 4L)) shouldBe listOf(1L, 2L)
    }

    @Test
    fun `resolveManageMergeTargetId keeps current target when it remains`() {
        resolveManageMergeTargetId(targetId = 1L, remainingIds = listOf(1L, 3L)) shouldBe 1L
    }

    @Test
    fun `resolveManageMergeTargetId promotes first remaining member when target is removed`() {
        resolveManageMergeTargetId(targetId = 1L, remainingIds = listOf(3L, 4L)) shouldBe 3L
    }

    @Test
    fun `resolveManageMergeTargetId returns null when no members remain`() {
        resolveManageMergeTargetId(targetId = 1L, remainingIds = emptyList()) shouldBe null
    }

    @Test
    fun `dialogRemainingIds respects changed root while removing staged members`() {
        val dialog = manageMergeDialog(
            targetId = 2L,
            memberIds = listOf(1L, 2L, 3L),
        )

        dialogRemainingIds(dialog, entryIdsToRemove = listOf(3L)) shouldBe listOf(1L, 2L)
        resolveManageMergeTargetId(targetId = dialog.targetId, remainingIds = listOf(1L, 2L)) shouldBe 2L
    }

    @Test
    fun `changing root allows previous root to be removed`() {
        val dialog = manageMergeDialog(
            targetId = 2L,
            memberIds = listOf(1L, 2L, 3L),
        )

        dialogRemainingIds(dialog, entryIdsToRemove = listOf(1L)) shouldBe listOf(2L, 3L)
        resolveManageMergeTargetId(targetId = dialog.targetId, remainingIds = listOf(2L, 3L)) shouldBe 2L
    }

    @Test
    fun `changed root remains in merge when removing previous root and library members`() {
        val dialog = manageMergeDialog(
            targetId = 2L,
            memberIds = listOf(1L, 2L, 3L, 4L),
        )

        dialogRemainingIds(dialog, entryIdsToRemove = listOf(1L, 4L)) shouldBe listOf(2L, 3L)
        resolveManageMergeTargetId(targetId = dialog.targetId, remainingIds = listOf(2L, 3L)) shouldBe 2L
    }

    private fun manageMergeDialog(targetId: Long, memberIds: List<Long>): EntryScreenModel.Dialog.ManageMerge {
        return EntryScreenModel.Dialog.ManageMerge(
            targetId = targetId,
            savedTargetId = targetId,
            members = memberIds.map { memberId ->
                EntryScreenModel.MergeMember(
                    id = memberId,
                    entry = Entry.create().copy(
                        id = memberId,
                        source = 1L,
                        title = "Entry $memberId",
                        initialized = true,
                    ),
                    subtitle = "",
                )
            }.toPersistentList(),
            removableIds = persistentListOf(),
            libraryRemovalIds = persistentListOf(),
        )
    }
}
