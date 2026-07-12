package eu.kanade.tachiyomi.ui.entry

import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.EntryChildListInteraction
import mihon.entry.interactions.EntryChildListRequest
import mihon.entry.interactions.EntryChildListRow
import mihon.entry.interactions.EntryChildProgressLabel
import mihon.entry.interactions.EntryChildProgressRequest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.model.UnifiedStubSource

class EntryScreenModelStateTest {

    @Test
    fun `showMergeNotice is true for a member entry opened outside the merged root`() {
        val state = successState(
            entry = entry(id = 2L),
            memberIds = persistentListOf(2L),
            mergeTargetId = 1L,
            mergeGroupMemberIds = persistentListOf(1L, 2L),
        )

        state.isPartOfMerge shouldBe true
        state.isMerged shouldBe false
        state.showMergeNotice shouldBe true
    }

    @Test
    fun `showMergeNotice is false for the merged root entry`() {
        val state = successState(
            entry = entry(id = 1L),
            memberIds = persistentListOf(1L, 2L),
            mergeTargetId = 1L,
            mergeGroupMemberIds = persistentListOf(1L, 2L),
        )

        state.isPartOfMerge shouldBe true
        state.isMerged shouldBe true
        state.showMergeNotice shouldBe false
    }

    @Test
    fun `showMergeNotice is false for a standalone entry`() {
        val state = successState(
            entry = entry(id = 3L),
            memberIds = persistentListOf(3L),
            mergeTargetId = 3L,
            mergeGroupMemberIds = persistentListOf(3L),
        )

        state.isPartOfMerge shouldBe false
        state.isMerged shouldBe false
        state.showMergeNotice shouldBe false
    }

    private fun successState(
        entry: Entry,
        memberIds: kotlinx.collections.immutable.ImmutableList<Long>,
        mergeTargetId: Long,
        mergeGroupMemberIds: kotlinx.collections.immutable.ImmutableList<Long>,
    ): EntryScreenModel.State.Success {
        return EntryScreenModel.State.Success(
            entry = entry,
            source = UnifiedStubSource(id = entry.source, lang = "en", name = "Test"),
            sourceName = "Test",
            isSourceMissing = false,
            memberIds = memberIds,
            memberTitleById = memberIds.associateWith { "Entry $it" },
            mergedMemberTitles = memberIds.map { "Entry $it" }.toPersistentList(),
            mergeTargetId = mergeTargetId,
            mergeGroupMemberIds = mergeGroupMemberIds,
            isFromSource = false,
            chapters = emptyList(),
            childListInteraction = NoOpEntryChildListInteraction,
            childGroupFilterSupported = false,
            availableScanlators = emptySet(),
            excludedScanlators = emptySet(),
        )
    }

    private fun entry(id: Long): Entry {
        return Entry.create().copy(
            id = id,
            source = 1L,
            title = "Entry $id",
            initialized = true,
            updateStrategy = EntryUpdateStrategy.ALWAYS_UPDATE,
        )
    }

    private object NoOpEntryChildListInteraction : EntryChildListInteraction {
        override fun sortedForReading(
            entry: Entry,
            chapters: List<EntryChapter>,
            memberIds: List<Long>,
        ): List<EntryChapter> {
            return chapters
        }

        override fun sortedForDisplay(
            entry: Entry,
            chapters: List<EntryChapter>,
            memberIds: List<Long>,
        ): List<EntryChapter> {
            return chapters
        }

        override fun buildDisplayList(request: EntryChildListRequest): List<EntryChildListRow> {
            return request.chapters.map(EntryChildListRow::Child)
        }

        override fun progressLabels(request: EntryChildProgressRequest): Flow<Map<Long, EntryChildProgressLabel>> {
            return flowOf(emptyMap())
        }
    }
}
