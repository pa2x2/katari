package app.katari.ui.automaticcheck

import app.katari.core.metro.AppCoroutineScope
import app.katari.core.metro.AutomaticCheck
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.base.OnboardingPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Makes every [AutomaticCheck] while the app is open, as [app.katari.ui.host.AppVisibility] tells: on Android while it
 * is on screen, where what a check finds can be offered, on desktop while its window is, hidden in the tray included.
 * Each check runs once the app is first open after onboarding, so a found update never lands on top of the first-run
 * setup, then whenever [CHECK_INTERVAL] has passed since, at once if that happened while the app was closed. A check
 * that started runs to its end when the app closes, so an update it installs isn't cut off halfway.
 */
@Inject
@SingleIn(AppScope::class)
class AutomaticChecks(
    // Lazy: the checks' owners, such as the extension manager, are built on the first check rather than on the main
    // thread that tells the app opened
    private val checks: Lazy<Set<AutomaticCheck>>,
    private val clock: SleepCountingClock,
    private val onboardingPreferences: OnboardingPreferences,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    private val isOpen = MutableStateFlow(false)

    private val schedules by lazy { checks.value.map(::Schedule) }

    init {
        scope.launch {
            isOpen.collectLatest { open ->
                if (!open) return@collectLatest
                onboardingPreferences.shownOnboardingFlow.changes().first { it }
                coroutineScope { schedules.forEach { launch { it.repeat() } } }
            }
        }
    }

    fun appOpened() {
        isOpen.value = true
    }

    fun appClosed() {
        isOpen.value = false
    }

    private inner class Schedule(private val check: AutomaticCheck) {
        /** [SleepCountingClock.elapsedMillis] at which the check next falls due; 0, long past, until it first runs. */
        @Volatile
        private var dueAt = 0L

        suspend fun repeat() {
            while (true) {
                val untilDue = dueAt - clock.elapsedMillis()
                if (untilDue > 0) {
                    // In steps rather than all at once: delay's clock stops while the device sleeps, the platform's
                    // doesn't, so a check that fell due during sleep runs soon after waking instead of hours later
                    delay(minOf(untilDue, CHECK_STEP.inWholeMilliseconds))
                    continue
                }
                // Set first, so a check still running from before the app last closed isn't started a second time
                dueAt = clock.elapsedMillis() + CHECK_INTERVAL.inWholeMilliseconds
                scope.launch {
                    // Soon rather than a whole interval later: a check made before the network is up, as after a
                    // login start or waking from sleep, fails
                    if (!check.check()) dueAt = clock.elapsedMillis() + CHECK_RETRY.inWholeMilliseconds
                }.join()
            }
        }
    }
}

private val CHECK_INTERVAL = 6.hours
private val CHECK_STEP = 15.minutes
private val CHECK_RETRY = 15.minutes
