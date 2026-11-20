package app.katari.appupdate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.katari.appupdate.AppUpdateController
import app.katari.appupdate.AppUpdatePreferences
import app.katari.appupdate.AppUpdateStatus
import app.katari.appupdate.check.AppUpdateChannel
import eu.kanade.presentation.more.settings.widget.ListPreferenceWidget
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/** About's update rows: the check with the update's state, and the two settings that shape it. */
fun appUpdateAboutRows(
    controller: AppUpdateController,
    preferences: AppUpdatePreferences,
): List<@Composable () -> Unit> = listOf(
    { CheckForUpdatesItem(controller, preferences) },
    { CheckOnLaunchItem(preferences) },
    { UpdateChannelItem(controller, preferences) },
)

@Composable
private fun CheckForUpdatesItem(controller: AppUpdateController, preferences: AppUpdatePreferences) {
    val state by controller.state.collectAsState()
    val lastCheckedAt by preferences.lastCheckedAt.collectAsState()
    val checking = state.status == AppUpdateStatus.Checking

    CheckForUpdatesRow(
        subtitle = state.status.subtitle(state.update, lastCheckedAt),
        checking = checking,
        onClick = {
            when {
                checking -> Unit
                state.update != null && state.status != AppUpdateStatus.UpToDate -> controller.openSheet()
                else -> controller.checkNow()
            }
        },
    )
}

@Composable
private fun CheckOnLaunchItem(preferences: AppUpdatePreferences) {
    val checkOnLaunch by preferences.checkOnLaunch.collectAsState()
    SwitchPreferenceWidget(
        title = stringResource(MR.strings.app_update_check_on_launch),
        subtitle = stringResource(MR.strings.app_update_check_on_launch_summary),
        checked = checkOnLaunch,
        onCheckedChanged = preferences.checkOnLaunch::set,
    )
}

@Composable
private fun UpdateChannelItem(controller: AppUpdateController, preferences: AppUpdatePreferences) {
    val channel by preferences.channel.collectAsState()
    val entries = mapOf(
        AppUpdateChannel.STABLE to stringResource(MR.strings.app_update_channel_stable),
        AppUpdateChannel.PRERELEASE to stringResource(MR.strings.app_update_channel_prerelease),
    )
    ListPreferenceWidget(
        value = channel,
        title = stringResource(MR.strings.app_update_channel),
        subtitle = entries.getValue(channel),
        icon = null,
        isProfileSpecific = false,
        entries = entries,
        onValueChange = {
            if (it != channel) {
                preferences.channel.set(it)
                controller.refresh()
            }
        },
    )
}
