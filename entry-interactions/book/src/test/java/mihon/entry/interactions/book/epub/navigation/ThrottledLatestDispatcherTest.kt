package mihon.entry.interactions.book.epub

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ThrottledLatestDispatcherTest {
    @Test
    fun `preview dispatches immediately then keeps only latest pending value`() = runTest {
        val values = mutableListOf<Int>()
        val dispatcher = ThrottledLatestDispatcher<Int>(
            scope = this,
            intervalMillis = 75L,
            dispatch = values::add,
        )

        dispatcher.preview(1)
        runCurrent()
        dispatcher.preview(2)
        dispatcher.preview(3)

        assertEquals(listOf(1), values)
        advanceTimeBy(75L)
        runCurrent()
        assertEquals(listOf(1, 3), values)
    }

    @Test
    fun `finish drops pending preview and dispatches exact final value`() = runTest {
        val values = mutableListOf<Int>()
        val dispatcher = ThrottledLatestDispatcher<Int>(
            scope = this,
            intervalMillis = 75L,
            dispatch = values::add,
        )

        dispatcher.preview(1)
        runCurrent()
        dispatcher.preview(2)
        dispatcher.finish(4)
        advanceTimeBy(75L)
        runCurrent()

        assertEquals(listOf(1, 4), values)
    }
}
