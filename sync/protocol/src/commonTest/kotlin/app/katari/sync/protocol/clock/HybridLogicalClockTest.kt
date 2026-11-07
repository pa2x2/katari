package app.katari.sync.protocol.clock

import app.katari.sync.protocol.clock.HybridLogicalClock.Receipt
import app.katari.sync.protocol.device.DeviceId
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Instant

class HybridLogicalClockTest {

    private val wall = SettableClock(millis = 1_700_000_000_000)
    private val clock = HybridLogicalClock(DeviceId(1), wall)

    @Test
    fun `timestamps keep increasing while the wall clock is behind and follow it again once it passes`() {
        val beforeStep = clock.now()
        wall.millis -= 60_000
        val first = clock.now()
        val second = clock.now()

        first shouldBeGreaterThan beforeStep
        second shouldBeGreaterThan first

        wall.millis += 120_000
        clock.now().physicalMillis shouldBe wall.millis
    }

    @Test
    fun `edits made after receiving a timestamp sort after it and older ones do not move the clock back`() {
        val local = clock.now()
        val sameMillisLaterCounter = HybridTimestamp(local.physicalMillis, counter = 5, device = DeviceId(2))
        clock.receive(sameMillisLaterCounter) shouldBe Receipt.Accepted
        clock.now() shouldBeGreaterThan sameMillisLaterCounter

        val ahead = HybridTimestamp(wall.millis + 30_000, counter = 7, device = DeviceId(2))
        clock.receive(ahead) shouldBe Receipt.Accepted
        clock.now() shouldBeGreaterThan ahead

        clock.receive(HybridTimestamp(wall.millis - 1, device = DeviceId(3))) shouldBe Receipt.Accepted
        clock.now() shouldBeGreaterThan ahead
    }

    @Test
    fun `a timestamp too far ahead is refused without moving the clock until the wall clock comes within the bound`() {
        val bound = HybridLogicalClock.MAX_AHEAD.inWholeMilliseconds
        val tooFar = HybridTimestamp(wall.millis + bound + 1, device = DeviceId(2))

        clock.receive(tooFar).shouldBeInstanceOf<Receipt.TooFarAhead>()
        clock.now() shouldBeLessThan tooFar

        wall.millis += 1
        clock.receive(tooFar) shouldBe Receipt.Accepted
        clock.now() shouldBeGreaterThan tooFar
    }

    private class SettableClock(var millis: Long) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
    }
}
