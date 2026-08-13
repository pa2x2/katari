package mihon.entry.interactions.host

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class AppEntryMergeDuplicateCandidateDataTest {

    @Test
    fun `chapter count observation chunks libraries beyond the SQL parameter boundary`() = runTest {
        val entryIds = (1L..1_002L).toList()
        val observedChunks = mutableListOf<List<Long>>()

        val counts = loadDuplicateCandidateCounts(entryIds) { chunk ->
            observedChunks += chunk
            chunk.associateWith { it * 2 }
        }

        observedChunks.map(List<Long>::size) shouldContainExactly listOf(500, 500, 2)
        counts.size shouldBe 1_002
        counts[1L] shouldBe 2L
        counts[1_002L] shouldBe 2_004L
    }

    @Test
    fun `chapter count changes reuse corpus while corpus changes refresh count ownership`() = runTest {
        val current = MutableStateFlow(entry(1, "Current", EntryType.BOOK, favorite = false))
        val book = entry(2, "Book", EntryType.BOOK)
        val anime = entry(3, "Anime", EntryType.ANIME)
        val library = MutableStateFlow(listOf(book, anime))
        val firstCounts = MutableStateFlow(mapOf(1L to 10L, 2L to 20L))
        val secondCounts = MutableStateFlow(mapOf(1L to 11L, 2L to 21L, 4L to 40L))
        val countFlows = ArrayDeque(listOf(firstCounts, secondCounts))
        val observedEntryIds = mutableListOf<List<Long>>()
        val snapshots = mutableListOf<DuplicateCandidateEntries>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeDuplicateCandidateEntries(current, library) { entryIds ->
                observedEntryIds += entryIds
                countFlows.removeFirst()
            }.toList(snapshots)
        }
        runCurrent()

        observedEntryIds shouldContainExactly listOf(listOf(2L, 1L))
        snapshots.single().run {
            this.library.map(Entry::id) shouldContainExactly listOf(2L)
            this.counts shouldBe mapOf(1L to 10L, 2L to 20L)
        }

        firstCounts.value = mapOf(1L to 11L, 2L to 21L)
        runCurrent()

        observedEntryIds shouldContainExactly listOf(listOf(2L, 1L))
        snapshots.last().counts shouldBe mapOf(1L to 11L, 2L to 21L)

        val secondBook = entry(4, "Second book", EntryType.BOOK)
        library.value = listOf(book, secondBook, anime)
        runCurrent()

        observedEntryIds shouldContainExactly listOf(listOf(2L, 1L), listOf(2L, 4L, 1L))
        snapshots.last().library.map(Entry::id) shouldContainExactly listOf(2L, 4L)
        snapshots.last().counts shouldBe mapOf(1L to 11L, 2L to 21L, 4L to 40L)

        val snapshotCount = snapshots.size
        firstCounts.value = mapOf(1L to 12L, 2L to 22L)
        runCurrent()
        snapshots.size shouldBe snapshotCount

        secondCounts.value = mapOf(1L to 13L, 2L to 23L, 4L to 41L)
        runCurrent()
        snapshots.last().counts shouldBe mapOf(1L to 13L, 2L to 23L, 4L to 41L)
    }

    private fun entry(
        id: Long,
        title: String,
        type: EntryType,
        favorite: Boolean = true,
    ): Entry {
        return Entry.create().copy(
            id = id,
            profileId = PROFILE_ID,
            source = SOURCE_ID,
            url = "/$id",
            title = title,
            favorite = favorite,
            type = type,
        )
    }

    private companion object {
        const val PROFILE_ID = 3L
        const val SOURCE_ID = 9L
    }
}
