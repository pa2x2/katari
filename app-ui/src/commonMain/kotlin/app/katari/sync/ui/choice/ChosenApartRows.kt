package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.setting.scope.MenuSettings
import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.PreferenceScopes
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The settings and menus a device chose apart from their areas: kept on it while their area syncs, or synced though
 * each device keeps its own by default.
 */
internal data class ChosenApart(val settings: Set<String>, val menus: Set<MenuSettings>) {
    operator fun plus(other: ChosenApart) = ChosenApart(settings + other.settings, menus + other.menus)

    val isEmpty: Boolean get() = settings.isEmpty() && menus.isEmpty()

    companion object {
        fun of(choices: ChoiceSet) = ChosenApart(choices.settingsOff + choices.settingsOn, choices.menusOff)

        val Saver: Saver<ChosenApart, Any> = listSaver(
            save = { listOf(it.settings.toList(), it.menus.map(MenuSettings::name)) },
            restore = { (settings, menus) ->
                ChosenApart(settings.toSet(), menus.mapTo(HashSet()) { MenuSettings.valueOf(it) })
            },
        )
    }
}

/**
 * What syncs' first group: each of [listed] with its switch and how it syncs now, in its area's order, so what was
 * chosen one by one, in popovers or on area pages, can be looked over and undone in one place. A row whose area doesn't
 * sync rests, as on the area's page. [titles] are the settings' titles by key ([settingTitles]).
 */
@Composable
internal fun chosenApartGroup(
    choices: ChoiceSet,
    listed: ChosenApart,
    titles: Map<String, String>,
    actions: WhatSyncsActions,
): Preference.PreferenceGroup? {
    if (listed.isEmpty) return null
    val settingRows = listed.settings.mapNotNull { key ->
        val mark = PreferenceScopes.markOf(key) ?: return@mapNotNull null
        val area = mark.area ?: return@mapNotNull null
        val title = titles[key] ?: key
        // As on the area's page: what the setting itself is set to, whether or not its area syncs now
        val on = if (mark.scope == PreferenceScope.DEVICE_BY_DEFAULT) {
            key in choices.settingsOn
        } else {
            key !in choices.settingsOff
        }
        val state = stringResource(
            when {
                mark.scope == PreferenceScope.DEVICE_BY_DEFAULT && on -> MR.strings.sync_choice_device_default_on
                mark.scope == PreferenceScope.DEVICE_BY_DEFAULT -> MR.strings.sync_choice_device_default_off
                on -> MR.strings.sync_choice_synced
                else -> MR.strings.sync_choice_off
            },
        )
        val row = chosenRow(title, detail(area.title(), state), on, choices.syncs(area)) {
            actions.syncSetting(key, title, it)
        }
        Triple(area.ordinal, title, row)
    }
    val menuRows = listed.menus.map { menu ->
        val area = PreferenceScopes.areaOf(menu)
        val title = menu.title()
        val on = choices.syncs(menu)
        val state = stringResource(if (on) MR.strings.sync_choice_synced else MR.strings.sync_choice_off)
        val row = chosenRow(title, detail(area.title(), state), on, choices.syncs(area)) {
            actions.syncMenu(menu, it)
        }
        Triple(area.ordinal, title, row)
    }
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.sync_choices_apart),
        preferenceItems = (settingRows + menuRows)
            .sortedWith(compareBy({ it.first }, { it.second }))
            .map { it.third },
    )
}

private fun chosenRow(title: String, subtitle: String?, on: Boolean, areaSynced: Boolean, onSync: (Boolean) -> Unit) =
    Preference.PreferenceItem.CustomPreference(title = title) {
        SwitchPreferenceWidget(
            title = title,
            subtitle = subtitle,
            checked = on,
            enabled = areaSynced,
            onCheckedChanged = onSync,
        )
    }
