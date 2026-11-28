package eu.kanade.tachiyomi.data.backup.conversion.mihon

import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Name
import eu.kanade.tachiyomi.data.backup.conversion.ConvertedSettings
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.*

/**
 * Mihon's app settings as Katari's, as the Mihon family and the Aniyomi family keep them; [app] is the app the report
 * names for a setting Katari has nothing for. Only what the backup holds is written: a setting it lacks takes Katari's
 * default, not Mihon's (D041).
 */
internal fun convertMihonSettings(backedUp: List<BackupPreference>, app: String): ConvertedSettings {
    val preferences = mutableListOf<BackupPreference>()
    val carried = mutableListOf<Item>()
    val leftBehind = mutableListOf<Item>()
    fun leaveBehind(name: Name, reason: StringResource) {
        leftBehind += Item(name, reason)
    }

    backedUp.forEach { preference ->
        val key = preference.key
        // A private setting has the same meaning as the one of the same name
        val name = Preference.stripPrivateKey(key)
        val tracker = MihonSettings.trackerSetting(key, name)
        val noCounterpart = MihonSettings.NO_COUNTERPART[name] ?: ForkSettings.NO_COUNTERPART[name]
        when {
            Preference.isAppState(key) || key in MihonSettings.IGNORED -> Unit
            key in MihonSettings.CONSENT ->
                leaveBehind(
                    MihonSettings.CONSENT.getValue(key).nameIn(app),
                    MR.strings.backup_conversion_consent_not_carried,
                )
            tracker != null -> leaveBehind(tracker.nameIn(app), MR.strings.backup_conversion_no_trackers)
            name in MihonSettings.CARRIED -> {
                preferences += preference
                carried += Item(Name.Setting(name))
            }
            name in MihonSettings.CARRIED_UNLISTED -> preferences += preference
            noCounterpart != null -> leaveBehind(noCounterpart.nameIn(app), MR.strings.backup_conversion_no_counterpart)
            else -> {
                val renamed = MihonSettings.renamed(preference)
                if (renamed == null) {
                    // A key no table knows, named by its key (D048, D118)
                    leaveBehind(Name.Text(key), MR.strings.backup_conversion_unknown_setting)
                } else {
                    preferences += renamed
                    val note = preference.tabletUiModeNote()
                    carried += renamed.map { Item(Name.Setting(it.key), note) }
                }
            }
        }
    }
    return ConvertedSettings(preferences, carried, leftBehind)
}

private fun BackupPreference.tabletUiModeNote(): StringResource? {
    if (key != MihonSettings.TABLET_UI_MODE_KEY) return null
    val mode = (value as? StringPreferenceValue)?.value
    return MR.strings.backup_conversion_tablet_ui_automatic.takeIf { TabletUiMode.entries.none { it.name == mode } }
}
