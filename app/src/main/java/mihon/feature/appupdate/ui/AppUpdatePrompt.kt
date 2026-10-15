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

/**
 * Hosts the update prompt in MainActivity and starts the automatic checks once onboarding is done, so a found update
 * never lands on top of the first-run setup. Rechecks run only while MainActivity is visible to show their prompt.
 */
@Composable
fun AppUpdatePrompt() {
    if (!AppUpdateBuild.isSupported) return
    val context = LocalContext.current
    val controller = remember { context.appGraph.appUpdateServices.controller }
    val basePreferences = remember { context.appGraph.basePreferences }
    val uriHandler = LocalUriHandler.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        basePreferences.shownOnboardingFlow.changes().first { it }
        controller.onLaunch()
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            controller.recheckPeriodically()
        }
    }

    val state by controller.state.collectAsState()
    val update = state.update
    if (state.sheetOpen && update != null) {
        AppUpdateSheet(
            update = update,
            status = state.status,
            onUpdate = controller::startUpdate,
            onCancelDownload = controller::cancelDownload,
            onAllowInstalls = { context.startActivity(controller.installPermissionIntent()) },
            onSkipVersion = controller::skipVersion,
            onOpenReleasePage = { uriHandler.openUri(update.pageUrl) },
            onDismiss = controller::closeSheet,
        )
    }
}
