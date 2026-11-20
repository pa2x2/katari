package app.katari.ui.automaticcheck

import app.katari.core.metro.AutomaticCheck
import eu.kanade.domain.base.OnboardingPreferences
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.FakePreferenceStore
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class AutomaticChecksTest {

    @Test
    fun `a check waits for its interval across reopening and makes none while the app is closed`() = runTest {
        var made = 0
        val checks = automaticChecks {
            made++
            true
        }

        checks.appOpened()
        runCurrent()
        made shouldBe 1

        checks.appClosed()
        advanceTimeBy(7.hours)
        made shouldBe 1

        // The interval passed while the app was closed
        checks.appOpened()
        runCurrent()
        made shouldBe 2

        checks.appClosed()
        checks.appOpened()
        advanceTimeBy(5.hours)
        made shouldBe 2
    }

    @Test
    fun `a failed check is made again within minutes`() = runTest {
        val results = ArrayDeque(listOf(false, true))
        var made = 0
        val checks = automaticChecks {
            made++
            results.removeFirst()
        }

        checks.appOpened()
        runCurrent()
        advanceTimeBy(16.minutes)
        made shouldBe 2

        advanceTimeBy(5.hours)
        made shouldBe 2
    }

    private fun TestScope.automaticChecks(check: AutomaticCheck): AutomaticChecks {
        val onboarding = OnboardingPreferences(FakePreferenceStore())
        onboarding.shownOnboardingFlow.set(true)
        return AutomaticChecks(
            checks = lazyOf(setOf(check)),
            clock = object : SleepCountingClock {
                override fun elapsedMillis(): Long = testScheduler.currentTime
            },
            onboardingPreferences = onboarding,
            scope = backgroundScope,
        )
    }
}
