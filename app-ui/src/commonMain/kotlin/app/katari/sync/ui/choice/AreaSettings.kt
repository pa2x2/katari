package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.katari.profiles.core.ProfileStore
import app.katari.sync.client.setting.scope.PreferenceMark
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.scope.SettingsArea
import app.katari.ui.di.LocalAppUiGraph
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.settingsScreens
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A setting a Settings screen shows: its key, its row's title, where it is, and how it syncs. [screen] names its Settings
 * screen where the area's settings are on several and this one isn't named after the area, such as "Manga reader"
 * among the Readers; [group] is the heading it is under there, such as "Cover badges".
 */
internal class AreaSetting(
    val key: String,
    val title: String,
    val screen: String?,
    val group: String?,
    val mark: PreferenceMark,
)

/**
 * The settings of [area] as the Settings screens show them, each once, in the screens' order and their rows'. The
 * screens' rows and the scope tables are the only lists of settings, so a setting added to a screen shows here too.
 */
@Composable
internal fun areaSettings(area: SettingsArea): List<AreaSetting> {
    val found = shownSettings().mapNotNull { (screenTitle, setting) ->
        // A menu's settings have one row for all of them
        val mark = PreferenceScopes.markOf(setting.key)?.takeIf { it.area == area && it.menu == null }
            ?: return@mapNotNull null
        Triple(screenTitle, setting, mark)
    }.distinctBy { (_, setting) -> setting.key }
    val onSeveralScreens = found.distinctBy { (screenTitle) -> screenTitle }.size > 1
    return found.map { (screenTitle, setting, mark) ->
        val screen = stringResource(screenTitle).takeIf { onSeveralScreens && screenTitle != area.titleRes }
        AreaSetting(setting.key, setting.title, screen, setting.group, mark)
    }
}

/**
 * Whether the area has a page listing its settings. Extension settings are each source's own, which no Settings screen
 * lists, so they sync or stay as a whole; an area that follows a data group goes with it.
 */
internal val SettingsArea.hasPage: Boolean
    get() = this in SettingsArea.switchable && this != SettingsArea.EXTENSION_SETTINGS

/** The title of each setting the Settings screens show, by its key: the first row that shows it. */
@Composable
internal fun settingTitles(): Map<String, String> =
    shownSettings().reversed().associate { (_, setting) -> setting.key to setting.title }

/** Every row of the Settings screens that stores its value in a preference, with its screen's title. */
@Composable
private fun shownSettings(): List<Pair<StringResource, StoredSetting>> {
    val graph = LocalAppUiGraph.current
    val screens = remember(graph) { settingsScreens(graph) }
    return screens.flatMap { screen ->
        val screenTitle = screen.getTitleRes()
        screen.getPreferences().storedSettings(graph.profileStore).map { screenTitle to it }
    }
}

private class StoredSetting(val key: String, val title: String, val group: String?)

/** Every row that stores its value in a preference, with the heading of the group it is in. */
private fun List<Preference>.storedSettings(profileStore: ProfileStore): List<StoredSetting> =
    flatMap { preference ->
        when (preference) {
            is Preference.PreferenceGroup -> preference.preferenceItems.map { it to preference.title }
            is Preference.PreferenceItem<*, *> -> listOf(preference to null)
        }
    }.mapNotNull { (item, group) ->
        item.storedPreference?.let {
            StoredSetting(profileStore.preferenceKey(it), item.title, group?.takeIf(String::isNotBlank))
        }
    }
