package app.katari.desktop.update

import app.katari.appupdate.AppUpdateController
import app.katari.core.metro.AppCoroutineScope
import app.katari.desktop.application.installation.AppInstallation
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.base.OnboardingPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The automatic update checks, for as long as the app runs, the window hidden in the tray included: they start once
 * onboarding is done, so a found update never lands on top of the first-run setup, and an update found while the
 * window is hidden tells of itself through [notification]. Only an [installation] that updates itself checks.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopAppUpdateChecks(
    private val installation: AppInstallation,
    private val controller: AppUpdateController,
    private val onboardingPreferences: OnboardingPreferences,
    private val notification: AppUpdateTrayNotification,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    fun start() {
        if (installation.update == null) return
        notification.start()
        scope.launch {
            onboardingPreferences.shownOnboardingFlow.changes().first { it }
            controller.onLaunch()
            controller.recheckPeriodically()
        }
    }
}
