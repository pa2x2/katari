package app.katari.sync.ui.arrival

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonSize
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.theme.TextRole
import app.katari.sync.client.arrival.ArrivedSetting
import app.katari.sync.client.arrival.KeepMine
import app.katari.sync.ui.choice.settingTitles
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScaffold
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Instant

/**
 * Sync → Changed on other devices: the settings other devices changed here in the last 14 days, by day, newest first,
 * each with Keep mine until it is kept, then Kept here while this device keeps it to itself. The settings of a menu that
 * one sync changed are one row, as they are kept as one.
 */
class ArrivedSettingsScreen : Screen() {

    @Composable
    override fun Content() {
        val backPress = LocalBackPress.current
        val model = metroViewModel<ArrivedSettingsViewModel>()
        val changes by model.changes.collectAsState()
        val titles = settingTitles()
        PreferenceScaffold(
            titleRes = MR.strings.sync_arrived_title,
            onBackPressed = backPress,
            itemsProvider = { arrivedPreferences(changes, titles, model::keepMine) },
        )
    }
}

@Composable
private fun arrivedPreferences(
    changes: List<ArrivedSetting>,
    titles: Map<String, String>,
    onKeepMine: (ArrivedSetting) -> Unit,
): List<Preference> {
    val rows = changes.groupBy { setting -> setting.menu?.let { it to setting.batch } ?: setting.id }.values
    val days = rows.groupBy { row ->
        Instant.fromEpochMilliseconds(row.first().changedAt).toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    return listOf(Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.sync_arrived_note))) +
        days.map { (day, dayRows) ->
            Preference.PreferenceGroup(
                title = relativeDateText(day),
                preferenceItems = dayRows.map { row -> arrivedRow(row, titles, onKeepMine) },
            )
        }
}

/** One setting, or the settings of one menu that one sync changed, with where it is and where it came from. */
@Composable
private fun arrivedRow(
    row: List<ArrivedSetting>,
    titles: Map<String, String>,
    onKeepMine: (ArrivedSetting) -> Unit,
): Preference.PreferenceItem<out Any, out Any> {
    val first = row.first()
    val area = first.areaTitle()
    val title = first.name(titles) ?: area ?: first.key
    val from = stringResource(MR.strings.sync_arrived_from, first.fromName())
    val subtitle = listOfNotNull(area.takeIf { it != title }, from).joinToString(" · ")
    val keepMine = if (row.any { it.keepMine == KeepMine.OFFERED }) KeepMine.OFFERED else first.keepMine
    return Preference.PreferenceItem.CustomPreference(title = title) {
        TextPreferenceWidget(
            title = title,
            subtitle = subtitle,
            widget = when (keepMine) {
                KeepMine.OFFERED -> {
                    {
                        Button(
                            label = stringResource(MR.strings.sync_keep_mine),
                            onClick = { onKeepMine(first) },
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
        )
    }
}
