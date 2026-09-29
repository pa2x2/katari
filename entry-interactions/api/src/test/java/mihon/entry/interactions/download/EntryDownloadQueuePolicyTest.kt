package mihon.entry.interactions.download

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
}
