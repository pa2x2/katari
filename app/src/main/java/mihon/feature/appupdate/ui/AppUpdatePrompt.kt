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
import mihon.feature.appupdate.AndroidAppUpdatePlatform
import mihon.feature.appupdate.AppUpdateBuild
import mihon.feature.appupdate.AppUpdateController
import mihon.feature.appupdate.AppUpdateStatus
import mihon.feature.appupdate.check.AvailableUpdate
import mihon.feature.appupdate.mpvaddon.MpvAddonController
import mihon.feature.appupdate.mpvaddon.MpvAddonRelease
import mihon.feature.appupdate.mpvaddon.restartKatari
import mihon.feature.appupdate.mpvaddon.ui.MpvAddonBroughtAlongNotes
import mihon.feature.appupdate.mpvaddon.ui.MpvAddonRestartDialog
import mihon.feature.appupdate.mpvaddon.ui.MpvAddonUpdateSheet
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.shortFileSize
import tachiyomi.presentation.core.i18n.stringResource

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
    val onboardingPreferences = remember { context.appGraph.onboardingPreferences }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        onboardingPreferences.shownOnboardingFlow.changes().first { it }
        services.mpvAddonController.onLaunch()
        if (!AppUpdateBuild.isSupported) return@LaunchedEffect
        services.controller.onLaunch()
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            services.controller.recheckPeriodically()
        }
    }

    val appState by services.controller.state.collectAsState()
    val appUpdate = appState.update
    val addon = appUpdate?.let(services.platform::addonOf)
    when {
        AppUpdateBuild.isSupported && appState.sheetOpen && appUpdate != null ->
            AppSheet(services.controller, services.platform, appUpdate, addon, appState.status)
        addon == null -> MpvAddonPrompts(services.mpvAddonController)
    }
}

/** Katari's update, saying so when it brings the mpv add-on along. */
@Composable
private fun AppSheet(
    controller: AppUpdateController,
    platform: AndroidAppUpdatePlatform,
    update: AvailableUpdate,
    addon: MpvAddonRelease?,
    status: AppUpdateStatus,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    AppUpdateSheet(
        update = update,
        status = status,
        onUpdate = controller::startUpdate,
        onCancelDownload = controller::cancelDownload,
        onAllowInstalls = { context.startActivity(platform.installPermissionIntent()) },
        onRestart = controller::restart,
        onSkipVersion = controller::skipVersion,
        onOpenReleasePage = { uriHandler.openUri(update.pageUrl) },
        onDismiss = controller::closeSheet,
        broughtAlongSummary = addon?.let {
            stringResource(MR.strings.mpv_addon_download_size, shortFileSize(it.apk.size))
        },
        broughtAlongNotes = addon?.let { { MpvAddonBroughtAlongNotes(it) } },
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
