package mihon.entry.interactions.child

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.runtime.DefaultEntryInteractions
import mihon.entry.interactions.runtime.EntryScanlatorFilterDataSource
import mihon.entry.interactions.runtime.EntryScanlatorFilterProcessor
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryScanlatorFilterFeatureTest {
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)

    @Test
    fun `snapshot and restore use the requested profile, and restore keeps existing exclusions`() = runTest {
        val dataSource = FakeDataSource(excluded = setOf("Group A"))
        val fixture = fixture(dataSource = dataSource)

        fixture.feature.snapshot(profileId = 9L, listOf(entry)) shouldBe
            mapOf(entry.id to EntryScanlatorFilterSnapshotResult.Available(setOf("Group A")))
        dataSource.lastReadProfileId shouldBe 9L

        fixture.feature.restore(profileId = 9L, entry, setOf("Group B")) shouldBe
            EntryScanlatorFilterRestoreResult.Restored(memberCount = 1)
        dataSource.lastWrite shouldBe Write(
            profileId = 9L,
            entryIds = listOf(entry.id),
            excluded = setOf("Group A", "Group B"),
        )
        fixture.feature.restore(profileId = 9L, entry, setOf("Group A", "Group B")) shouldBe
            EntryScanlatorFilterRestoreResult.NoChange
    }

    private fun fixture(dataSource: FakeDataSource = FakeDataSource()): Fixture {
        val interactions = DefaultEntryInteractions(
            scanlatorFilterProcessors = mapOf(EntryType.BOOK to TestScanlatorFilterProcessor),
        )
        return Fixture(
            feature = DefaultEntryScanlatorFilterFeature(
                applicableTypes = setOf(EntryType.BOOK),
                interaction = interactions.scanlatorFilter,
                dataSource = dataSource,
            ),
        )
    }

    private data class Fixture(
        val feature: EntryScanlatorFilterFeature,
    )

    private object TestScanlatorFilterProcessor : EntryScanlatorFilterProcessor {
        override fun scanlatorFor(entry: Entry, chapter: EntryChapter): String? {
            return normalizeScanlator(entry, chapter.scanlator.orEmpty())
        }

        override fun normalizeScanlator(entry: Entry, scanlator: String): String? {
            return scanlator.trim().takeIf(String::isNotEmpty)
        }
    }

    private class FakeDataSource(
        private var excluded: Set<String> = emptySet(),
    ) : EntryScanlatorFilterDataSource {
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

        override fun excludedScanlatorsChanged(profileId: Long?, entryIds: Collection<Long>): Flow<Unit> {
            return excludedChanges
        }

        override suspend fun excludedScanlators(profileId: Long?, entryIds: Collection<Long>): Map<Long, Set<String>> {
            lastReadProfileId = profileId
            return entryIds.associateWith { excluded }
        }

        override suspend fun setExcludedScanlators(
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
