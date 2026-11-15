package app.katari.desktop.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import app.katari.appupdate.AppUpdateController
import app.katari.appupdate.ui.AppUpdateSheet
import app.katari.desktop.application.installation.AppInstallation

/**
 * Hosts the update prompt over the home, which [DesktopAppUpdateChecks] opens. Only an [installation] that updates
 * itself shows it.
 */
@Composable
fun DesktopAppUpdatePrompt(installation: AppInstallation, controller: AppUpdateController) {
    if (installation.update == null) return
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
