package app.katari.sync.ui.arrival

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonSize
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.theme.TextRole
import app.katari.sync.client.arrival.ArrivedSetting
import app.katari.sync.client.arrival.KeepMine
import app.katari.sync.ui.arrival.change.ChangeLookups
import app.katari.sync.ui.arrival.change.line
import app.katari.sync.ui.choice.settingRows
import app.katari.ui.di.LocalAppUiGraph
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScaffold
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Instant

/**
 * Sync → Changed on other devices: the settings other devices changed here in the last 14 days, by day, newest first,
 * each saying what changed, with Keep mine until it is kept, then Kept here while this device keeps it to itself. A
 * menu's changes are one row, as they are kept as one, dated by the newest; it opens to list them all.
 */
class ArrivedSettingsScreen : Screen() {

    @Composable
    override fun Content() {
        val backPress = LocalBackPress.current
        val model = metroViewModel<ArrivedSettingsViewModel>()
        val changes by model.changes.collectAsState()
        val lookups = LocalAppUiGraph.current.changeLookups
        PreferenceScaffold(
            titleRes = MR.strings.sync_arrived_title,
            onBackPressed = backPress,
            itemsProvider = { arrivedPreferences(changes, lookups, model::keepMine) },
        )
    }
}

@Composable
private fun arrivedPreferences(
    changes: List<ArrivedSetting>,
    lookups: ChangeLookups,
    onKeepMine: (ArrivedSetting) -> Unit,
): List<Preference> {
    val settingRows = settingRows()
    // Settings of several profiles say whose each is
    val profileNamed = changes.mapNotNull { it.profileId }.distinct().size > 1
    val days = arrivedRows(changes, settingRows, lookups).groupBy { row ->
        Instant.fromEpochMilliseconds(row.newest.batch).toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    return listOf(Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.sync_arrived_note))) +
        days.map { (day, dayRows) ->
            Preference.PreferenceGroup(
                title = relativeDateText(day),
                preferenceItems = dayRows.map { row ->
                    val title = row.newest.name(settingRows, lookups) ?: row.newest.areaTitle() ?: row.newest.key
                    Preference.PreferenceItem.CustomPreference(title = title) {
                        val profile = row.newest.profileId?.takeIf { profileNamed }?.let(lookups::profileName)
                        ArrivedRowContent(row, title, profile, onKeepMine)
                    }
                },
            )
        }
}

/**
 * One setting, or a menu's changes, with what changed, whose it is ([profile], when the page lists several profiles'),
 * where it is and where it came from.
 */
@Composable
private fun ArrivedRowContent(
    row: ArrivedRow,
    title: String,
    profile: String?,
    onKeepMine: (ArrivedSetting) -> Unit,
) {
    val newest = row.newest
    val area = newest.areaTitle()
    val parts = row.parts.orEmpty()
    val origin = listOfNotNull(
        profile,
        area.takeIf { it != title },
        stringResource(MR.strings.sync_arrived_from, newest.fromName()),
    ).joinToString(" · ")
    // Only a row whose line names some items of a list ("and 4 more") opens
    val opens = parts.any { it.full != it.text }
    var expanded by rememberSaveable(newest.id) { mutableStateOf(false) }
    Column {
        SettingsRow(
            title = title,
            subtitle = parts.takeIf { it.isNotEmpty() }?.line(title),
            below = { Text(origin, role = TextRole.Supporting) },
            trailing = when (row.keepMine) {
                KeepMine.OFFERED -> {
                    {
                        Button(
                            label = stringResource(MR.strings.sync_keep_mine),
                            onClick = { onKeepMine(newest) },
                            style = ButtonStyle.Tonal,
                            size = ButtonSize.Small,
                        )
                    }
                }
                KeepMine.KEPT_HERE -> {
                    { Text(stringResource(MR.strings.sync_kept_here), role = TextRole.Supporting) }
                }
                KeepMine.SYNCED_AGAIN -> null
            },
            expanded = expanded.takeIf { opens },
            onClick = if (opens) {
                { expanded = !expanded }
            } else {
                null
            },
        )
        if (opens && expanded) {
            parts.forEach { part -> SettingsRow(title = part.name, subtitle = part.full) }
        }
    }
}
