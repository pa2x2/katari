package mihon.entry.interactions.child

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.runtime.EntryChildGroupFilterCapability
import mihon.entry.interactions.runtime.EntryChildGroupFilterDataSource
import mihon.entry.interactions.runtime.EntryChildGroupFilterProcessor
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryChildGroupFilterFeatureTest {
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)

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

    private fun fixture(dataSource: FakeDataSource = FakeDataSource()): Fixture {
        val bindings = listOf(EntryChildGroupFilterCapability.bind(TestChildGroupFilterProcessor))
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
        private var excluded: Set<String> = emptySet(),
    ) : EntryChildGroupFilterDataSource {
        private val childrenChanges = MutableSharedFlow<Unit>()
        private val excludedChanges = MutableSharedFlow<Unit>()
        var lastReadProfileId: Long? = null
        var lastWrite: Write? = null

        override fun childrenChanged(entryIds: Collection<Long>): Flow<Unit> {
            return childrenChanges
        }

        override suspend fun children(entryIds: Collection<Long>): List<EntryChapter> {
            return emptyList()
        }

        override fun excludedGroupsChanged(profileId: Long?, entryIds: Collection<Long>): Flow<Unit> {
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

    private data class Write(
        val profileId: Long?,
        val entryIds: List<Long>,
        val excluded: Set<String>,
    )
}
