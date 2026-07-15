package mihon.entry.interactions.viewer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EntryChildTransitionTest {
    @Test
    fun `builds window around current child`() {
        val window = listOf(1L, 2L, 3L).entryChildWindow(2L)

        assertEquals(1L, window?.previous)
        assertEquals(2L, window?.current)
        assertEquals(3L, window?.next)
    }

    @Test
    fun `builds window using stable child key`() {
        val children = listOf(Child(1L), Child(2L), Child(3L))

        val window = children.entryChildWindow(2L, Child::id)

        assertEquals(1L, window?.previous?.id)
        assertEquals(2L, window?.current?.id)
        assertEquals(3L, window?.next?.id)
    }

    @Test
    fun `returns no window when current child is absent`() {
        assertNull(listOf(1L, 2L, 3L).entryChildWindow(9L))
    }

    @Test
    fun `crossed transition retains boundary identity`() {
        val forward = EntryChildTransition.Next(from = 1L, to = 2L)
        val backward = EntryChildTransition.Prev(from = 2L, to = 1L)

        assertEquals(forward, backward)
        assertEquals(forward.hashCode(), backward.hashCode())
    }

    @Test
    fun `terminal boundaries remain direction sensitive`() {
        val previous = EntryChildTransition.Prev<Long>(from = 1L, to = null)
        val next = EntryChildTransition.Next<Long>(from = 1L, to = null)

        assertNotEquals(previous, next)
    }

    private data class Child(val id: Long)
}
