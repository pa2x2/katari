package app.katari.sync.ui.arrival

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.katari.presentation.kit.feedback.LocalAppMessages
import app.katari.presentation.kit.feedback.MessageAction
import app.katari.sync.client.arrival.ArrivedSetting
import app.katari.sync.ui.SyncUiGraph
import app.katari.sync.ui.choice.settingTitles
import app.katari.sync.ui.choice.title
import app.katari.ui.di.LocalAppUiGraph
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Tells the settings each sync changes here from other devices while the app is open, once, as a message on the screen
 * the person is on, with Keep mine keeping them all ([app.katari.sync.client.arrival.SettingArrivals]). Sync → Changed
 * on other devices lists them afterwards.
 */
@Composable
fun SettingArrivalNotices(graph: SyncUiGraph = LocalAppUiGraph.current) {
    val messages = LocalAppMessages.current
    val scope = rememberCoroutineScope()
    var arrived by remember { mutableStateOf<List<ArrivedSetting>?>(null) }
    LaunchedEffect(graph) { graph.settingArrivals.arrivals().collect { if (it.isNotEmpty()) arrived = it } }
    val told = arrived ?: return
    val text = arrivalText(told)
    val keepMine = stringResource(MR.strings.sync_keep_mine)
    LaunchedEffect(told) {
        val batch = told.first().batch
        messages.show(
            text,
            long = true,
            action = MessageAction(keepMine) {
                scope.launch { graph.settingArrivals.keepMine(batch) }
            },
        )
        arrived = null
    }
}

/** What one sync changed, by its name when it changed one setting or one menu, and the device it came from. */
@Composable
private fun arrivalText(arrived: List<ArrivedSetting>): String {
    val first = arrived.first()
    val device = if (arrived.all { it.from == first.from }) {
        first.fromName()
    } else {
        stringResource(MR.strings.sync_arrived_other_devices)
    }
    val menu = first.menu?.takeIf { menu -> arrived.all { it.menu == menu } }
    val name = menu?.title() ?: first.name(settingTitles()).takeIf { arrived.size == 1 }
    val area = first.areaTitle().takeIf { arrived.size == 1 }
    return when {
        name != null -> stringResource(MR.strings.sync_arrived_named, name, device)
        area != null -> stringResource(MR.strings.sync_arrived_area, area, device)
        else -> pluralStringResource(MR.plurals.sync_arrived_settings, arrived.size, arrived.size, device)
    }
}
