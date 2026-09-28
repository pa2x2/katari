package mihon.entry.interactions.viewer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class EntryChildTransitionTest {
    @Test
    fun `crossed transition keeps boundary identity while terminal boundaries stay direction sensitive`() {
        val forward = EntryChildTransition.Next(from = 1L, to = 2L)
        val backward = EntryChildTransition.Prev(from = 2L, to = 1L)

        assertEquals(forward, backward)
        assertEquals(forward.hashCode(), backward.hashCode())
        assertNotEquals(
            EntryChildTransition.Prev<Long>(from = 1L, to = null),
            EntryChildTransition.Next<Long>(from = 1L, to = null),
        )
    }
}
