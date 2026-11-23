package app.katari.sync.ui.arrival

import androidx.compose.runtime.Composable
import app.katari.sync.client.arrival.ArrivedSetting
import app.katari.sync.client.arrival.KeepMine
import app.katari.sync.client.setting.scope.MenuSettings
import app.katari.sync.ui.arrival.change.ChangeLookups
import app.katari.sync.ui.arrival.change.ChangePart
import app.katari.sync.ui.arrival.change.arrivedChanges
import eu.kanade.presentation.more.settings.Preference

/**
 * The settings one row of Changed on other devices tells, newest first: one setting, or every change to one menu in one
 * profile that stands alike for Keep mine, whichever sync brought it, as a menu's settings sync or stay as one. [parts]
 * say what changed; null when that is unknown.
 */
internal class ArrivedRow(val settings: List<ArrivedSetting>, val parts: List<ChangePart>?) {
    val newest: ArrivedSetting
        get() = settings.first()

    val keepMine: KeepMine
        get() = newest.keepMine
}

/**
 * [changes], newest first, as the rows that tell them in that order. A row whose settings read as they did before is
 * left out: another device changed them and changed them back, or wrote the value this device read by default.
 */
@Composable
internal fun arrivedRows(
    changes: List<ArrivedSetting>,
    settingRows: Map<String, Preference.PreferenceItem<*, *>>,
    lookups: ChangeLookups,
): List<ArrivedRow> =
    changes.groupBy { setting -> setting.menu?.let { MenuRow(it, setting.profileId, setting.keepMine) } ?: setting.id }
        .values
        .mapNotNull { settings ->
            val parts = arrivedChanges(settings, settingRows, lookups)
            ArrivedRow(settings, parts).takeUnless { parts?.isEmpty() == true }
        }

private data class MenuRow(val menu: MenuSettings, val profileId: Long?, val keepMine: KeepMine)
