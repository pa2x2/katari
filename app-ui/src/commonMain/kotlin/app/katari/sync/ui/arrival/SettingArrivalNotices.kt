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
import app.katari.sync.ui.arrival.change.ChangeLookups
import app.katari.sync.ui.arrival.change.itemsText
import app.katari.sync.ui.arrival.change.line
import app.katari.sync.ui.choice.settingRows
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
    val text = arrivalText(told, graph.changeLookups)
    val keepMine = stringResource(MR.strings.sync_keep_mine)
    LaunchedEffect(told) {
        if (text != null) {
            val batch = told.first().batch
            messages.show(
                text,
                long = true,
                action = MessageAction(keepMine) {
                    scope.launch { graph.settingArrivals.keepMine(batch) }
                },
            )
        }
        arrived = null
    }
}

/**
 * What one sync changed and the device it came from: what happened when it is one setting or menu and that fits in one
 * part ("From expertbook: Hid MangaDex and Comick"), else the names of the parts that changed, else how many settings.
 * Null when every setting reads as it did before ([arrivedRows]).
 */
@Composable
private fun arrivalText(arrived: List<ArrivedSetting>, lookups: ChangeLookups): String? {
    val settingRows = settingRows()
    val rows = arrivedRows(arrived, settingRows, lookups)
    val told = rows.flatMap { it.settings }.ifEmpty { return null }
    val first = told.first()
    val device = if (told.all { it.from == first.from }) {
        first.fromName()
    } else {
        stringResource(MR.strings.sync_arrived_other_devices)
    }
    val single = rows.singleOrNull()
    val parts = single?.parts.orEmpty()
    val part = parts.singleOrNull()?.takeIf { it.full == it.text }
    val name = when {
        parts.isNotEmpty() -> itemsText(parts.map { it.name }.distinct())
        single != null -> first.name(settingRows, lookups)
        else -> null
    }
    val area = first.areaTitle().takeIf { single != null }
    return when {
        part != null -> stringResource(MR.strings.sync_arrived_change_from, device, parts.line(rowTitle = null))
        name != null -> stringResource(MR.strings.sync_arrived_named, name, device)
        area != null -> stringResource(MR.strings.sync_arrived_area, area, device)
        else -> pluralStringResource(MR.plurals.sync_arrived_settings, told.size, told.size, device)
    }
}
