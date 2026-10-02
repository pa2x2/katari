package mihon.feature.appupdate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import eu.kanade.domain.base.BasePreferences
import kotlinx.coroutines.flow.first
import mihon.feature.appupdate.AppUpdateBuild
import mihon.feature.appupdate.AppUpdateController
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Hosts the update prompt in MainActivity and starts the launch check once onboarding is done, so a found update
 * never lands on top of the first-run setup.
 */
@Composable
fun AppUpdatePrompt() {
    if (!AppUpdateBuild.isSupported) return
    val controller = remember { Injekt.get<AppUpdateController>() }
    val basePreferences = remember { Injekt.get<BasePreferences>() }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        basePreferences.shownOnboardingFlow.changes().first { it }
        controller.onLaunch()
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
