package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.scope.SettingsArea
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What this device keeps to itself, in a few words: "Everything", or "All but Statistics, Readers settings, Library
 * sort, 2 settings, Work profile". Menus and single settings count only where their area syncs, which names them
 * otherwise. [profiles] names the profiles it doesn't sync, which [choices] knows only by their identities.
 */
@Composable
internal fun whatSyncsSummary(choices: ChoiceSet, profiles: List<String>): String {
    val off = buildList {
        DataGroup.entries.filterNot(choices::syncs).forEach { add(it.title()) }
        SettingsArea.switchable.filterNot(choices::syncs).forEach {
            add(stringResource(MR.strings.sync_choices_area_settings, it.title()))
        }
        choices.menusOff.filter { choices.syncs(PreferenceScopes.areaOf(it)) }.forEach { add(it.title()) }
        val keptSettings = choices.settingsOff.count { key ->
            PreferenceScopes.markOf(key)?.area?.let(choices::syncs) == true
        }
        if (keptSettings > 0) {
            add(pluralStringResource(MR.plurals.sync_choices_settings_count, keptSettings, keptSettings))
        }
        profiles.forEach { add(stringResource(MR.strings.sync_choices_profile, it)) }
    }
    return if (off.isEmpty()) {
        stringResource(MR.strings.sync_choices_everything)
    } else {
        stringResource(MR.strings.sync_choices_all_but, off.joinToString())
    }
}
