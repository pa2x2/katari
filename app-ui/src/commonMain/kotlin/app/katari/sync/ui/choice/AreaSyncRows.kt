package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.setting.scope.MenuSettings
import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.scope.SettingsArea
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * An area's page: its switch, then its [settings] in three groups: those it syncs, with its menus' settings, those that
 * stay on each device unless chosen, and those that only work here, each with why. A setting's switch rests while the area doesn't sync.
 */
@Composable
internal fun areaSyncPreferences(
    area: SettingsArea,
    choices: ChoiceSet,
    settings: List<AreaSetting>,
    onSyncArea: (Boolean) -> Unit,
    onSyncSetting: (key: String, on: Boolean) -> Unit,
    onSyncMenu: (MenuSettings, on: Boolean) -> Unit,
): List<Preference> {
    val synced = choices.syncs(area)
    // Where a setting is tells apart settings of the same name, such as each reader's own, and says what a title
    // such as "Entry type" is about
    val subtitle: @Composable (AreaSetting, String?) -> String? = { setting, state ->
        detail(setting.screen, setting.group, state)
    }
    val byScope = settings.groupBy { it.mark.scope }

    val areaTitle = stringResource(MR.strings.sync_area_switch, area.title())
    val areaRow = Preference.PreferenceItem.CustomPreference(title = areaTitle) {
        SwitchPreferenceWidget(title = areaTitle, checked = synced, onCheckedChanged = onSyncArea)
    }
    val syncedRows = byScope[PreferenceScope.PERSON].orEmpty().map { setting ->
        val on = setting.key !in choices.settingsOff
        val state = if (on) null else stringResource(MR.strings.sync_choice_off)
        settingRow(setting, subtitle(setting, state), on, synced, onSyncSetting)
    }
    // A menu's settings sync or stay together, after the settings Settings lists one by one
    val menuRows = MenuSettings.entries.filter { PreferenceScopes.areaOf(it) == area }.map { menu ->
        val title = menu.title()
        val on = choices.syncs(menu)
        val state = if (on) null else stringResource(MR.strings.sync_choice_off)
        val subtitle = detail(menu.place(), state)
        Preference.PreferenceItem.CustomPreference(title = title) {
            SwitchPreferenceWidget(
                title = title,
                subtitle = subtitle,
                checked = on,
                enabled = synced,
                onCheckedChanged = { onSyncMenu(menu, it) },
            )
        }
    }
    val chosenRows = byScope[PreferenceScope.DEVICE_BY_DEFAULT].orEmpty().map { setting ->
        val on = setting.key in choices.settingsOn
        val state = stringResource(
            if (on) MR.strings.sync_choice_device_default_on else MR.strings.sync_choice_device_default_off,
        )
        settingRow(setting, subtitle(setting, state), on, synced, onSyncSetting)
    }
    val deviceRows = byScope[PreferenceScope.DEVICE].orEmpty().map { setting ->
        val reason = setting.mark.bound?.reason()
        Preference.PreferenceItem.CustomPreference(title = setting.title) {
            TextPreferenceWidget(
                title = setting.title,
                subtitle = subtitle(setting, reason),
                widget = { LockedMark() },
                enabled = false,
            )
        }
    }
    return listOfNotNull(
        areaRow,
        Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.sync_choice_area_off_note))
            .takeUnless { synced },
        group(stringResource(MR.strings.sync_choice_group_synced), syncedRows + menuRows),
        group(stringResource(MR.strings.sync_choice_group_device_default), chosenRows),
        group(stringResource(MR.strings.sync_choice_group_device_only), deviceRows),
    )
}

/** A row's line from its parts, such as where it is and whether it syncs. */
@Composable
private fun detail(vararg parts: String?): String? = parts.filterNotNull().reduceOrNull { detail, part ->
    stringResource(MR.strings.sync_choice_setting_detail, detail, part)
}

private fun settingRow(
    setting: AreaSetting,
    subtitle: String?,
    on: Boolean,
    areaSynced: Boolean,
    onSyncSetting: (key: String, on: Boolean) -> Unit,
) = Preference.PreferenceItem.CustomPreference(title = setting.title) {
    SwitchPreferenceWidget(
        title = setting.title,
        subtitle = subtitle,
        checked = on,
        enabled = areaSynced,
        onCheckedChanged = { onSyncSetting(setting.key, it) },
    )
}

private fun group(title: String, rows: List<Preference.PreferenceItem<out Any, out Any>>) =
    rows.takeIf { it.isNotEmpty() }?.let { Preference.PreferenceGroup(title = title, preferenceItems = it) }
