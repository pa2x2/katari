package mihon.entry.interactions.viewer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EntryChildTransitionAnchorTest {
    @Test
    fun `centered actionable transition wins while viewport can move`() {
        val centered = Item.Boundary(EntryChildTransition.Next(1L, 2L), actionable = true)

        assertEquals(centered, resolve(centered, null, null, true, true))
    }

    @Test
    fun `visible hard edge activates compact transition outside center`() {
        val boundary = Item.Boundary(EntryChildTransition.Next(1L, 2L), actionable = true)

        assertEquals(boundary, resolve(Item.Content, Item.Content, boundary, true, false))
    }

    @Test
    fun `non-scrollable content chooses actionable boundary instead of centered terminal`() {
        val previous = Item.Boundary(EntryChildTransition.Prev(2L, 1L), actionable = true)
        val terminal = Item.Boundary(EntryChildTransition.Next<Long>(2L, null), actionable = false)

        assertEquals(previous, resolve(terminal, previous, terminal, false, false))
    }

    @Test
    fun `non-actionable transition does not activate`() {
        val loaded = Item.Boundary(EntryChildTransition.Next(1L, 2L), actionable = false)

        assertNull(resolve(loaded, loaded, loaded, true, true))
    }

    private fun resolve(
        centered: Item?,
        first: Item?,
        last: Item?,
        canScrollBackward: Boolean,
        canScrollForward: Boolean,
    ): Item? = entryChildTransitionItemAtAnchor(
        centeredItem = centered,
        firstVisibleItem = first,
        lastVisibleItem = last,
        canScrollBackward = canScrollBackward,
        canScrollForward = canScrollForward,
        transitionOf = { (it as? Item.Boundary)?.transition },
        isActionable = { item, _ -> (item as Item.Boundary).actionable },
    )

    private sealed interface Item {
        data object Content : Item

        data class Boundary(
            val transition: EntryChildTransition<Long>,
            val actionable: Boolean,
        ) : Item
    }
}
