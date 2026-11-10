package app.katari.entry.interactions.media.session

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.time.Duration

class ReadingClockTest {

    @Test
    fun `a chapter change while the reader is behind another window neither records nor starts time`() {
        val clock = ReadingClock()
        clock.start()
        clock.stop()
        Thread.sleep(5)

        clock.lap() shouldBe Duration.ZERO
        Thread.sleep(5)
        clock.stop() shouldBe Duration.ZERO
    }
}
