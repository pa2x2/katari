package mihon.feature.appupdate.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.widget.ListPreferenceWidget
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import mihon.app.di.appGraph
import mihon.feature.appupdate.AppUpdateBuild
import mihon.feature.appupdate.AppUpdateStatus
import mihon.feature.appupdate.check.AppUpdateChannel
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * About's update rows: the check with the update's state, and the two settings that shape it. Empty when this build
 * can't update itself.
 */
fun appUpdateAboutRows(): List<@Composable () -> Unit> {
    if (!AppUpdateBuild.isSupported) return emptyList()
    return listOf({ CheckForUpdatesItem() }, { CheckOnLaunchItem() }, { UpdateChannelItem() })
}

@Composable
private fun CheckForUpdatesItem() {
    val context = LocalContext.current
    val controller = remember { context.appGraph.appUpdateServices.controller }
    val preferences = remember { context.appGraph.appUpdateServices.preferences }
    val state by controller.state.collectAsState()
    val lastCheckedAt by preferences.lastCheckedAt.collectAsState()
    val checking = state.status == AppUpdateStatus.Checking

    TextPreferenceWidget(
        title = stringResource(MR.strings.check_for_updates),
        subtitle = state.status.subtitle(state.update, lastCheckedAt),
        widget = {
            AnimatedVisibility(visible = checking) {
                LoadingIndicator(modifier = Modifier.size(KatariTheme.sizes.icon))
            }
        },
        onPreferenceClick = {
            when {
                checking -> Unit
                state.update != null && state.status != AppUpdateStatus.UpToDate -> controller.openSheet()
                else -> controller.checkNow()
            }
        },
    )
}

@Composable
private fun CheckOnLaunchItem() {
    val context = LocalContext.current
    val preferences = remember { context.appGraph.appUpdateServices.preferences }
    val checkOnLaunch by preferences.checkOnLaunch.collectAsState()
    SwitchPreferenceWidget(
        title = stringResource(MR.strings.app_update_check_on_launch),
        subtitle = stringResource(MR.strings.app_update_check_on_launch_summary),
        checked = checkOnLaunch,
        onCheckedChanged = preferences.checkOnLaunch::set,
    )
}

@Composable
private fun UpdateChannelItem() {
    val context = LocalContext.current
    val controller = remember { context.appGraph.appUpdateServices.controller }
    val preferences = remember { context.appGraph.appUpdateServices.preferences }
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
