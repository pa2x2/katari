package eu.kanade.presentation.more.settings.screen.animeplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import app.katari.appupdate.mpvaddon.MpvAddonController
import app.katari.appupdate.mpvaddon.MpvAddonUpdateCheck
import app.katari.appupdate.mpvaddon.MpvAddonUpdatePreferences
import app.katari.appupdate.ui.CheckForUpdatesRow
import app.katari.appupdate.ui.checkedTime
import app.katari.video.engine.mpv.install.describe
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/** Checks for a newer add-on than the installed one, which opens the add-on's update sheet when it finds one. */
@Composable
internal fun MpvAddonUpdateCheckRow(controller: MpvAddonController, preferences: MpvAddonUpdatePreferences) {
    val context = LocalContext.current
    val check by controller.check.collectAsState()
    val prompt by controller.prompt.collectAsState()
    val lastCheckedAt by preferences.lastCheckedAt.collectAsState()
    val update = prompt.update
    val checking = check == MpvAddonUpdateCheck.Checking

    CheckForUpdatesRow(
        subtitle = when {
            checking -> stringResource(MR.strings.app_update_checking)
            update != null -> stringResource(MR.strings.app_update_version_available, update.release.version)
            check == MpvAddonUpdateCheck.UpToDate ->
                stringResource(MR.strings.app_update_up_to_date, checkedTime(lastCheckedAt))
            else -> (check as? MpvAddonUpdateCheck.Failed)?.error?.describe(context)
                ?: lastCheckedAt.takeIf { it > 0 }
                    ?.let { stringResource(MR.strings.app_update_last_checked, checkedTime(it)) }
        },
        checking = checking,
        onClick = {
            when {
                checking -> Unit
                update != null -> controller.openSheet()
                else -> controller.checkNow()
            }
        },
    )
}
