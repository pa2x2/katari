package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryChildGroupFilterFeatureTest {
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)
    private val first = chapter(id = 11L, entryId = 7L, group = "Group A")
    private val second = chapter(id = 12L, entryId = 8L, group = " Group B ")
    private val ungrouped = chapter(id = 13L, entryId = 8L, group = " ")

    @Test
    fun `provider absence is valid and every operation is structurally inapplicable`() = runTest {
        val fixture = fixture(provider = false)
        val scope = EntryChildGroupFilterScope(entry, listOf(entry.id))

        fixture.feature.isApplicable(entry.type) shouldBe false
        fixture.feature.state(scope) shouldBe EntryChildGroupFilterStateResult.Inapplicable(entry.type)
        fixture.feature.observe(scope) shouldBe EntryChildGroupFilterObservationResult.Inapplicable(entry.type)
        fixture.feature.filter(entry, listOf(first), setOf("Group A")) shouldBe
            EntryChildGroupFilterResult.Inapplicable(entry.type)
        fixture.feature.setExcludedGroups(scope, setOf("Group A")) shouldBe
            EntryChildGroupFilterMutationResult.Inapplicable(entry.type)
        fixture.feature.snapshot(profileId = 3L, entry) shouldBe
            EntryChildGroupFilterSnapshotResult.Inapplicable(entry.type)
        fixture.feature.restore(entry, setOf("Group A")) shouldBe
            EntryChildGroupFilterRestoreResult.Inapplicable(entry.type)
    }

    @Test
    fun `one provider activates state observation filtering and multi-member persistence`() = runTest {
        val dataSource = FakeDataSource(
            chapters = listOf(first, second, ungrouped),
            excluded = setOf("Group A"),
        )
        val fixture = fixture(dataSource = dataSource)
        val scope = EntryChildGroupFilterScope(entry, listOf(7L, 8L, 7L))

        fixture.feature.isApplicable(entry.type) shouldBe true
        fixture.feature.state(scope) shouldBe EntryChildGroupFilterStateResult.Available(
            EntryChildGroupFilterState(
                availableGroups = setOf("Group A", "Group B"),
                excludedGroups = setOf("Group A"),
            ),
        )

        val observation = fixture.feature.observe(scope) as EntryChildGroupFilterObservationResult.Available
        observation.states.first() shouldBe EntryChildGroupFilterState(
            availableGroups = setOf("Group A", "Group B"),
            excludedGroups = setOf("Group A"),
        )
        dataSource.observedChildIds shouldBe listOf(7L, 8L)
        dataSource.observedExcludedIds shouldBe listOf(7L, 8L)

        fixture.feature.filter(entry, listOf(first, second, ungrouped), setOf(" Group A ")) shouldBe
            EntryChildGroupFilterResult.Available(listOf(second, ungrouped))

        fixture.feature.setExcludedGroups(scope, setOf("Group B")) shouldBe
            EntryChildGroupFilterMutationResult.Applied(memberCount = 2)
        dataSource.lastWrite shouldBe Write(profileId = null, entryIds = listOf(7L, 8L), excluded = setOf("Group B"))
    }

    @Test
    fun `snapshot uses requested profile and restore merges without replacing existing groups`() = runTest {
        val dataSource = FakeDataSource(excluded = setOf("Group A"))
        val fixture = fixture(dataSource = dataSource)

        fixture.feature.snapshot(profileId = 9L, entry) shouldBe
            EntryChildGroupFilterSnapshotResult.Available(setOf("Group A"))
        dataSource.lastReadProfileId shouldBe 9L

        fixture.feature.restore(entry, setOf("Group B")) shouldBe
            EntryChildGroupFilterRestoreResult.Restored(memberCount = 1)
        dataSource.lastWrite shouldBe Write(
            profileId = null,
            entryIds = listOf(entry.id),
            excluded = setOf("Group A", "Group B"),
        )
        fixture.feature.restore(entry, setOf("Group A", "Group B")) shouldBe
            EntryChildGroupFilterRestoreResult.NoChange
    }

    private fun fixture(
        provider: Boolean = true,
        dataSource: FakeDataSource = FakeDataSource(),
    ): Fixture {
        val bindings = if (provider) {
            listOf(EntryChildGroupFilterCapability.bind(TestChildGroupFilterProcessor))
        } else {
            emptyList()
        }
        val composition = createEntryInteractionComposition(
            plugins = listOf(
                object : EntryInteractionPlugin {
                    override val type = EntryType.BOOK
                    override val owner = ContributionOwner("test.type.book")
                    override val providerBindings = bindings
                },
            ),
            featureContributors = listOf(EntryChildGroupFilterFeatureContributor),
        )
        return Fixture(
            feature = DefaultEntryChildGroupFilterFeature(
                evaluation = composition.featureGraphEvaluation,
                interaction = composition.interactions.childGroupFilter,
                dataSource = dataSource,
            ),
        )
    }

    private data class Fixture(
        val feature: EntryChildGroupFilterFeature,
    )

    private object TestChildGroupFilterProcessor : EntryChildGroupFilterProcessor {
        override val type = EntryType.BOOK

        override fun groupFor(entry: Entry, chapter: EntryChapter): String? {
            return normalizeGroup(entry, chapter.scanlator.orEmpty())
        }

        override fun normalizeGroup(entry: Entry, group: String): String? {
            return group.trim().takeIf(String::isNotEmpty)
        }
    }

    private class FakeDataSource(
        private val chapters: List<EntryChapter> = emptyList(),
        private var excluded: Set<String> = emptySet(),
    ) : EntryChildGroupFilterDataSource {
        private val childrenChanges = MutableSharedFlow<Unit>()
        private val excludedChanges = MutableSharedFlow<Unit>()
        var observedChildIds: List<Long>? = null
        var observedExcludedIds: List<Long>? = null
        var lastReadProfileId: Long? = null
        var lastWrite: Write? = null

        override fun childrenChanged(entryIds: Collection<Long>): Flow<Unit> {
            observedChildIds = entryIds.toList()
            return childrenChanges
        }

        override suspend fun children(entryIds: Collection<Long>): List<EntryChapter> {
            return chapters.filter { it.entryId in entryIds }
        }

        override fun excludedGroupsChanged(profileId: Long?, entryIds: Collection<Long>): Flow<Unit> {
            observedExcludedIds = entryIds.toList()
            return excludedChanges
        }

        override suspend fun excludedGroups(profileId: Long?, entryIds: Collection<Long>): Map<Long, Set<String>> {
            lastReadProfileId = profileId
            return entryIds.associateWith { excluded }
        }

        override suspend fun setExcludedGroups(
            profileId: Long?,
            entryIds: Collection<Long>,
            excluded: Set<String>,
        ) {
            this.excluded = excluded
            lastWrite = Write(profileId, entryIds.toList(), excluded)
        }
    }

    private fun chapter(id: Long, entryId: Long, group: String): EntryChapter {
        return EntryChapter.create().copy(id = id, entryId = entryId, scanlator = group)
    }

    private data class Write(
        val profileId: Long?,
        val entryIds: List<Long>,
        val excluded: Set<String>,
    )
}
