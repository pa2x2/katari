package eu.kanade.tachiyomi.ui.entry

import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.EntryChildGroupFilterFeature
import mihon.entry.interactions.EntryChildGroupFilterMutationResult
import mihon.entry.interactions.EntryChildGroupFilterObservationResult
import mihon.entry.interactions.EntryChildGroupFilterRestoreResult
import mihon.entry.interactions.EntryChildGroupFilterResult
import mihon.entry.interactions.EntryChildGroupFilterScope
import mihon.entry.interactions.EntryChildGroupFilterSnapshotResult
import mihon.entry.interactions.EntryChildGroupFilterStateResult
import mihon.entry.interactions.EntryChildListDisplay
import mihon.entry.interactions.EntryChildListFeature
import mihon.entry.interactions.EntryChildListRequest
import mihon.entry.interactions.EntryChildListResult
import mihon.entry.interactions.EntryChildListRow
import mihon.entry.interactions.EntryChildOrderResult
import mihon.entry.interactions.EntryChildProgressLabel
import mihon.entry.interactions.EntryChildProgressRequest
import mihon.entry.interactions.EntryChildProgressResult
import mihon.entry.interactions.EntryFirstChildResult
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
            childListFeature = TestEntryChildListFeature,
            childGroupFilterFeature = TestEntryChildGroupFilterFeature,
            childGroupFilterState = EntryChildGroupFilterStateResult.Inapplicable(entry.type),
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

    private object TestEntryChildListFeature : EntryChildListFeature {
        override fun isApplicable(type: eu.kanade.tachiyomi.source.entry.EntryType): Boolean = true

        override fun readingOrder(
            entry: Entry,
            chapters: List<EntryChapter>,
            memberIds: List<Long>,
        ): EntryChildOrderResult = EntryChildOrderResult.Available(chapters)

        override fun firstReadingChild(
            entry: Entry,
            chapters: List<EntryChapter>,
            memberIds: List<Long>,
        ): EntryFirstChildResult = EntryFirstChildResult.Available(chapters.firstOrNull())

        override fun displayOrder(
            entry: Entry,
            chapters: List<EntryChapter>,
            memberIds: List<Long>,
        ): EntryChildOrderResult = EntryChildOrderResult.Available(chapters)

        override fun displayList(request: EntryChildListRequest): EntryChildListResult {
            return EntryChildListResult.Available(
                EntryChildListDisplay(
                    rows = request.chapters.map(EntryChildListRow::Child),
                    aggregateMissingCount = 0,
                ),
            )
        }

        override fun progressLabels(request: EntryChildProgressRequest): EntryChildProgressResult {
            val labels: Flow<Map<Long, EntryChildProgressLabel>> = flowOf(emptyMap())
            return EntryChildProgressResult.Available(labels)
        }
    }

    private object TestEntryChildGroupFilterFeature : EntryChildGroupFilterFeature {
        override fun isApplicable(type: eu.kanade.tachiyomi.source.entry.EntryType): Boolean = false

        override suspend fun state(scope: EntryChildGroupFilterScope): EntryChildGroupFilterStateResult {
            return EntryChildGroupFilterStateResult.Inapplicable(scope.entry.type)
        }

        override fun observe(scope: EntryChildGroupFilterScope): EntryChildGroupFilterObservationResult {
            return EntryChildGroupFilterObservationResult.Inapplicable(scope.entry.type)
        }

        override fun filter(
            entry: Entry,
            chapters: List<EntryChapter>,
            excludedGroups: Set<String>,
        ): EntryChildGroupFilterResult = EntryChildGroupFilterResult.Inapplicable(entry.type)

        override suspend fun setExcludedGroups(
            scope: EntryChildGroupFilterScope,
            excludedGroups: Set<String>,
        ): EntryChildGroupFilterMutationResult = EntryChildGroupFilterMutationResult.Inapplicable(scope.entry.type)

        override suspend fun snapshot(
            profileId: Long,
            entry: Entry,
        ): EntryChildGroupFilterSnapshotResult = EntryChildGroupFilterSnapshotResult.Inapplicable(entry.type)

        override suspend fun restore(
            entry: Entry,
            excludedGroups: Set<String>,
        ): EntryChildGroupFilterRestoreResult = EntryChildGroupFilterRestoreResult.Inapplicable(entry.type)
    }
}
