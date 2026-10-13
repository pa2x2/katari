package eu.kanade.tachiyomi.ui.reader.loader.scheduling

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test

class PageLoadChangesTest {

    @Test
    fun `turning a page keeps the downloads ahead while a jump hands their slots to the new position`() {
        // Pages 0 to 11 are loaded and 12 to 15 download while page 10 is read
        val loaded = (0..11).toSet()
        val loading = (12..15).toSet()
        fun scheduleAround(index: Int) = schedulePageLoads(
            wanted = readAheadOrder(index, pageCount = 40, wholeChapter = true),
            loading = loading,
            missing = { it !in loaded && it !in loading },
            slots = 4,
        )

        val pageTurn = scheduleAround(11)
        pageTurn.cancel.shouldBeEmpty()
        pageTurn.start.shouldBeEmpty()

        val jump = scheduleAround(30)
        jump.cancel.shouldContainExactlyInAnyOrder(12, 13, 14, 15)
        jump.start.shouldContainExactly(30, 29, 31, 32)
    }
}
