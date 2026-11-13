package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CloudSync
import app.katari.presentation.kit.button.IconToggleButton
import app.katari.sync.client.setting.scope.SettingsArea
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/*
 * A Settings screen's sync view: the screen's own rows and groups, in its order, with each setting's control replaced by
 * whether it syncs, so a whole screen is chosen without leaving it. Rows that store no setting, such as links to other
 * screens, step aside while it shows.
 */

/**
 * The app bar action showing or hiding the sync view of a Settings screen with [items], while this device is linked
 * and the screen has a setting that could sync.
 */
@Composable
fun SettingsSyncViewAction(items: List<Preference>, showing: Boolean, onShowingChange: (Boolean) -> Unit) {
    val marks = rememberSettingSyncMarks() ?: return
    if (items.storedItems().none { marks.of(it) != null }) return
    IconToggleButton(
        icon = MaterialSymbols.Rounded.CloudSync,
        contentDescription = stringResource(MR.strings.sync_choices_title),
        checked = showing,
        onCheckedChange = onShowingChange,
    )
}

/** [items] as the sync view shows them; [items] themselves while this device isn't linked. */
@Composable
fun settingsSyncPreferences(items: List<Preference>): List<Preference> {
    val marks = rememberSettingSyncMarks() ?: return items
    val model = metroViewModel<WhatSyncsViewModel>()
    val choices by model.choices.collectAsState()
    fun row(item: Preference.PreferenceItem<*, *>): Preference.PreferenceItem.CustomPreference? {
        val mark = marks.of(item)?.takeIf { item.visible } ?: return null
        return settingsSyncRow(mark, settingSyncState(mark.key, choices) ?: return null, model)
    }
    val rows = items.mapNotNull { preference ->
        when (preference) {
            is Preference.PreferenceGroup -> preference.preferenceItems.mapNotNull(::row)
                .takeIf { it.isNotEmpty() }
                ?.let { preference.copy(preferenceItems = it) }
            is Preference.PreferenceItem<*, *> -> row(preference)
        }
    }
    // The areas switched off as a whole, whose own switch leads the screen
    val areasOff = items.storedItems().filter { it.visible }
        .mapNotNull { item -> marks.of(item)?.let { settingSyncState(it.key, choices) } }
        .filterIsInstance<SettingSyncState.AreaOff>()
        .map { it.area }
        .distinct()
    return listOf(Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.sync_view_note))) +
        areasOff.map { areaRow(it, model) } +
        rows
}

/**
 * Asks, while the sync view shows, whose values settings switched back on keep; elsewhere a setting's popover asks in
 * place.
 */
@Composable
fun SettingsSyncViewQuestion() {
    SettingsAgainQuestion(metroViewModel<WhatSyncsViewModel>())
}

private fun SettingSyncMarks.of(item: Preference.PreferenceItem<*, *>): SettingSyncMark? =
    item.storedPreference?.let { of(it, item.title) }

private fun List<Preference>.storedItems(): List<Preference.PreferenceItem<*, *>> = flatMap { preference ->
    when (preference) {
        is Preference.PreferenceGroup -> preference.preferenceItems
        is Preference.PreferenceItem<*, *> -> listOf(preference)
    }
}

private fun settingsSyncRow(mark: SettingSyncMark, state: SettingSyncState, model: WhatSyncsViewModel) =
    Preference.PreferenceItem.CustomPreference(title = mark.title) {
        when (state) {
            is SettingSyncState.Switch -> SwitchPreferenceWidget(
                title = mark.title,
                subtitle = state.summary(),
                checked = state.on,
                onCheckedChanged = { model.syncSetting(mark.key, mark.title, it) },
            )
            is SettingSyncState.Menu -> SwitchPreferenceWidget(
                title = mark.title,
                subtitle = stringResource(MR.strings.sync_choice_setting_detail, state.menu.title(), state.summary()),
                checked = state.on,
                onCheckedChanged = { model.syncMenu(state.menu, it) },
            )
            is SettingSyncState.AreaOff -> SwitchPreferenceWidget(
                title = mark.title,
                subtitle = state.summary(),
                checked = false,
                enabled = false,
                onCheckedChanged = {},
            )
            is SettingSyncState.WithData -> TextPreferenceWidget(title = mark.title, subtitle = state.summary())
            is SettingSyncState.DeviceOnly -> TextPreferenceWidget(
                title = mark.title,
                subtitle = state.summary(),
                widget = { LockedMark() },
                enabled = false,
            )
        }
    }

private fun areaRow(area: SettingsArea, model: WhatSyncsViewModel) =
    Preference.PreferenceItem.CustomPreference(title = area.name) {
        val title = stringResource(MR.strings.sync_area_switch, area.title())
        SwitchPreferenceWidget(title = title, checked = false, onCheckedChanged = { model.syncArea(area, it) })
    }
