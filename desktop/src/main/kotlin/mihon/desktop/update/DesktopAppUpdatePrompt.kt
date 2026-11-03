package mihon.desktop.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import eu.kanade.domain.base.OnboardingPreferences
import kotlinx.coroutines.flow.first
import mihon.feature.appupdate.AppUpdateController
import mihon.feature.appupdate.ui.AppUpdateSheet

/**
 * Hosts the update prompt over the home and starts the automatic checks once onboarding is done, so a found update
 * never lands on top of the first-run setup. The window is the app, so the checks repeat for as long as it is open.
 */
@Composable
fun DesktopAppUpdatePrompt(controller: AppUpdateController, onboardingPreferences: OnboardingPreferences) {
    if (!RunningAppImage.canUpdate) return
    LaunchedEffect(Unit) {
        onboardingPreferences.shownOnboardingFlow.changes().first { it }
        controller.onLaunch()
        controller.recheckPeriodically()
    }
    val state by controller.state.collectAsState()
    val update = state.update
    if (!state.sheetOpen || update == null) return
    val uriHandler = LocalUriHandler.current
    AppUpdateSheet(
        update = update,
        status = state.status,
        onUpdate = controller::startUpdate,
        onCancelDownload = controller::cancelDownload,
        // Desktop installs need no permission
        onAllowInstalls = {},
        onRestart = controller::restart,
        onSkipVersion = controller::skipVersion,
        onOpenReleasePage = { uriHandler.openUri(update.pageUrl) },
        onDismiss = controller::closeSheet,
    )
}
