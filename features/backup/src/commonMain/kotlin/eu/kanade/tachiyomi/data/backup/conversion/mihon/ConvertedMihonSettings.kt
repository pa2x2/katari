package eu.kanade.tachiyomi.data.backup.conversion.mihon

import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Name
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.*

/** A Mihon backup's app settings as Katari's, with what the report lists of them. */
internal class ConvertedMihonSettings(
    val preferences: List<BackupPreference>,
    val carried: List<Item>,
    val leftBehind: List<Item>,
)

/** Only what the backup holds is written: a setting it lacks takes Katari's default, not Mihon's (D041). */
internal fun convertMihonSettings(backedUp: List<BackupPreference>): ConvertedMihonSettings {
    val preferences = mutableListOf<BackupPreference>()
    val carried = mutableListOf<Item>()
    val leftBehind = mutableListOf<Item>()
    fun leaveBehind(key: String, reason: StringResource) {
        leftBehind += Item(Name.Setting(key), reason)
    }

    backedUp.forEach { preference ->
        val key = preference.key
        // A private setting has the same meaning as the one of the same name
        val name = Preference.stripPrivateKey(key)
        when {
            Preference.isAppState(key) || key in MihonSettings.IGNORED -> Unit
            key in MihonSettings.CONSENT -> leaveBehind(key, MR.strings.backup_conversion_consent_not_carried)
            name in MihonSettings.TRACKER_KEYS || MihonSettings.TRACKER_KEY_PREFIXES.any(key::startsWith) -> {
                leaveBehind(key, MR.strings.backup_conversion_no_trackers)
            }
            name in MihonSettings.CARRIED -> {
                preferences += preference
                carried += Item(Name.Setting(key))
            }
            name in MihonSettings.CARRIED_UNLISTED -> preferences += preference
            name in MihonSettings.NO_COUNTERPART -> leaveBehind(key, MR.strings.backup_conversion_no_counterpart)
            else -> {
                val renamed = MihonSettings.renamed(preference)
                if (renamed == null) {
                    // A key outside Mihon's own is a fork's setting, which the report names by its key (D048)
                    leftBehind += Item(Name.Text(key), MR.strings.backup_conversion_app_own_setting)
                } else {
                    preferences += renamed
                    carried += Item(Name.Setting(key), preference.tabletUiModeNote())
                }
            }
        }
    }
    return ConvertedMihonSettings(preferences, carried, leftBehind)
}

private fun BackupPreference.tabletUiModeNote(): StringResource? {
    if (key != MihonSettings.TABLET_UI_MODE_KEY) return null
    val mode = (value as? StringPreferenceValue)?.value
    return MR.strings.backup_conversion_tablet_ui_automatic.takeIf { TabletUiMode.entries.none { it.name == mode } }
}
