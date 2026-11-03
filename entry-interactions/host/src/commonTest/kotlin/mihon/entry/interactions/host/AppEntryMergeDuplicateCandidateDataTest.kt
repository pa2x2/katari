package mihon.entry.interactions.host

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

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
}
