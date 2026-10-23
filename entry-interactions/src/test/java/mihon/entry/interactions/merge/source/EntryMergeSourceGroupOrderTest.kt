package mihon.entry.interactions.merge.source

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EntryMergeSourceGroupOrderTest {
    @Test
    fun `relisting keeps members a user added in place while locked members follow the source`() {
        val root = 1L
        val (a, b, c) = Triple(10L, 11L, 12L)
        val (userFirst, userSecond) = 20L to 21L
        val current = listOf(root, a, userFirst, b, userSecond)

        sourceGroupOrder(current, root, listOf(a, c, b), claimed = setOf(a, b, c), unlinked = emptySet()) shouldBe
            listOf(root, a, c, userFirst, b, userSecond)
        sourceGroupOrder(current, root, listOf(b, a), claimed = setOf(a, b), unlinked = setOf(c)) shouldBe
            listOf(root, b, userFirst, a, userSecond)
    }
}
