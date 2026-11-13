package app.katari.sync.ui.arrival

import androidx.compose.runtime.Composable
import app.katari.sync.client.arrival.ArrivedSetting
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.ui.choice.title
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The device the setting came from, by its name, or "another device" when the account doesn't list it any more. */
@Composable
internal fun ArrivedSetting.fromName(): String = from ?: stringResource(MR.strings.sync_arrived_another_device)

/**
 * What the setting is called where the person changes it: its menu's title for a menu's setting, else its title in
 * Settings ([titles], from `settingTitles()`), or null where neither names it.
 */
@Composable
internal fun ArrivedSetting.name(titles: Map<String, String>): String? = menu?.title() ?: titles[key]

/** The title of the Settings area the setting belongs to. */
@Composable
internal fun ArrivedSetting.areaTitle(): String? = PreferenceScopes.markOf(key)?.area?.title()
