package eu.kanade.tachiyomi.util.system

import androidx.work.WorkInfo
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class PeriodicWorkClockTest {

    @Test
    fun `a schedule the clock left months ahead restarts, but a period or backoff on a steady clock is kept`() {
        val period = 6.hours

        periodicWork(period, nextRunIn = 72.days).isScheduledBeyondItsPeriod(NOW) shouldBe true
        periodicWork(period, nextRunIn = period - 10.minutes).isScheduledBeyondItsPeriod(NOW) shouldBe false
        periodicWork(1.hours, nextRunIn = 5.hours, runAttemptCount = 6).isScheduledBeyondItsPeriod(NOW) shouldBe false
        periodicWork(1.days, nextRunIn = 1.days + 20.hours, initialDelay = 20.hours)
            .isScheduledBeyondItsPeriod(NOW) shouldBe false
    }

    private fun periodicWork(
        period: Duration,
        nextRunIn: Duration,
        initialDelay: Duration = Duration.ZERO,
        runAttemptCount: Int = 0,
    ) = WorkInfo(
        id = UUID.randomUUID(),
        state = WorkInfo.State.ENQUEUED,
        tags = emptySet(),
        runAttemptCount = runAttemptCount,
        initialDelayMillis = initialDelay.inWholeMilliseconds,
        periodicityInfo = WorkInfo.PeriodicityInfo(period.inWholeMilliseconds, 10.minutes.inWholeMilliseconds),
        nextScheduleTimeMillis = NOW + nextRunIn.inWholeMilliseconds,
    )

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
