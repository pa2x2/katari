package mihon.entry.interactions

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class EntryDownloadQueuePolicyTest {

    @Test
    fun `reorder pending preserves active work even when the requested order moves or omits it`() {
        val queue = listOf("active-a", "first", "active-b", "second")

        val result = EntryDownloadQueuePolicy.reorderPending(
            queue = queue,
            requested = listOf("second", "active-b", "first"),
            keyOf = { it },
            isActive = { it.startsWith("active") },
        )

        result.shouldContainExactly("active-a", "active-b", "second", "first")
    }

    @Test
    fun `promote moves selected work ahead while retaining queue order`() {
        val queue = listOf("active", "first", "selected-a", "second", "selected-b")

        val result = EntryDownloadQueuePolicy.promote(
            queue = queue,
            keys = listOf("selected-b", "selected-a"),
            keyOf = { it },
            isActive = { it == "active" },
        )

        result.shouldContainExactly("active", "selected-a", "selected-b", "first", "second")
    }

    @Test
    fun `promote ignores missing and duplicate keys`() {
        val queue = listOf(1L, 2L, 3L)

        val result = EntryDownloadQueuePolicy.promote(
            queue = queue,
            keys = listOf(2L, 2L, 9L),
            keyOf = { it },
        )

        result.shouldContainExactly(2L, 1L, 3L)
    }
}
