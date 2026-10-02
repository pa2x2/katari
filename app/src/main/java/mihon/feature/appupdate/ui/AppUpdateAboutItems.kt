package mihon.feature.appupdate.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.widget.ListPreferenceWidget
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import mihon.feature.appupdate.AppUpdateBuild
import mihon.feature.appupdate.AppUpdateController
import mihon.feature.appupdate.AppUpdatePreferences
import mihon.feature.appupdate.AppUpdateStatus
import mihon.feature.appupdate.check.AppUpdateChannel
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** About's update rows: the check with the update's state, and the two settings that shape it. */
fun LazyListScope.appUpdateAboutItems() {
    if (!AppUpdateBuild.isSupported) return
    item { CheckForUpdatesItem() }
    item { CheckOnLaunchItem() }
    item { UpdateChannelItem() }
}

@Composable
private fun CheckForUpdatesItem() {
    val controller = remember { Injekt.get<AppUpdateController>() }
    val preferences = remember { Injekt.get<AppUpdatePreferences>() }
    val state by controller.state.collectAsState()
    val lastCheckedAt by preferences.lastCheckedAt.collectAsState()
    val checking = state.status == AppUpdateStatus.Checking

    TextPreferenceWidget(
        title = stringResource(MR.strings.check_for_updates),
        subtitle = state.status.subtitle(state.update, lastCheckedAt),
        widget = {
            AnimatedVisibility(visible = checking) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
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
    val preferences = remember { Injekt.get<AppUpdatePreferences>() }
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
    val controller = remember { Injekt.get<AppUpdateController>() }
    val preferences = remember { Injekt.get<AppUpdatePreferences>() }
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
