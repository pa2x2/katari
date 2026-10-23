package mihon.feature.appupdate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.first
import mihon.app.di.appGraph
import mihon.feature.appupdate.AppUpdateBuild
import mihon.feature.appupdate.AppUpdateController
import mihon.feature.appupdate.AppUpdateStatus
import mihon.feature.appupdate.check.AvailableUpdate
import mihon.feature.appupdate.mpvaddon.MpvAddonController
import mihon.feature.appupdate.mpvaddon.restartKatari
import mihon.feature.appupdate.mpvaddon.ui.MpvAddonRestartDialog
import mihon.feature.appupdate.mpvaddon.ui.MpvAddonUpdateSheet

/**
 * Hosts the update prompts in MainActivity and starts the automatic checks once onboarding is done, so a found update
 * never lands on top of the first-run setup. Rechecks run only while MainActivity is visible to show their prompt.
 *
 * Katari's own update comes first; the mpv add-on's sheet waits for it, and doesn't show when Katari's update brings
 * the add-on along. Builds that can't update themselves still keep the add-on current.
 */
@Composable
fun AppUpdatePrompt() {
    val context = LocalContext.current
    val services = remember { context.appGraph.appUpdateServices }
    val basePreferences = remember { context.appGraph.basePreferences }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        basePreferences.shownOnboardingFlow.changes().first { it }
        services.mpvAddonController.onLaunch()
        if (!AppUpdateBuild.isSupported) return@LaunchedEffect
        services.controller.onLaunch()
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            services.controller.recheckPeriodically()
        }
    }

    val appState by services.controller.state.collectAsState()
    val appUpdate = appState.update
    when {
        AppUpdateBuild.isSupported && appState.sheetOpen && appUpdate != null ->
            AppSheet(services.controller, appUpdate, appState.status)
        appUpdate?.addon == null -> MpvAddonPrompts(services.mpvAddonController)
    }
}

@Composable
private fun AppSheet(controller: AppUpdateController, update: AvailableUpdate, status: AppUpdateStatus) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    AppUpdateSheet(
        update = update,
        status = status,
        onUpdate = controller::startUpdate,
        onCancelDownload = controller::cancelDownload,
        onAllowInstalls = { context.startActivity(controller.installPermissionIntent()) },
        onSkipVersion = controller::skipVersion,
        onOpenReleasePage = { uriHandler.openUri(update.pageUrl) },
        onDismiss = controller::closeSheet,
    )
}

@Composable
private fun MpvAddonPrompts(controller: MpvAddonController) {
    val context = LocalContext.current
    val prompt by controller.prompt.collectAsState()
    val install by controller.state.collectAsState()
    val update = prompt.update
    if (prompt.sheetOpen && update != null) {
        MpvAddonUpdateSheet(
            update = update,
            status = install.status,
            onUpdate = controller::install,
            onCancelDownload = controller::cancel,
            onAllowInstalls = { context.startActivity(controller.installPermissionIntent()) },
            onSkipVersion = controller::skipVersion,
            onDismiss = controller::closeSheet,
        )
    }
    if (prompt.restartOpen) {
        MpvAddonRestartDialog(
            onRestart = { restartKatari(context) },
            onDismiss = controller::closeRestartPrompt,
        )
    }
}
