package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.katari.profiles.core.ProfileStore
import app.katari.sync.client.setting.scope.PreferenceMark
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.scope.SettingsArea
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.settingsScreens
import tachiyomi.presentation.core.i18n.stringResource

/** A setting a Settings screen shows: its key, its row's title, the screen's title, and how it syncs. */
internal class AreaSetting(val key: String, val title: String, val screen: String, val mark: PreferenceMark)

/**
 * The settings of [area] as the Settings screens show them, each once, in the screens' order and their rows'. The
 * screens' rows and the scope tables are the only lists of settings, so a setting added to a screen shows here too.
 */
@Composable
internal fun areaSettings(area: SettingsArea): List<AreaSetting> {
    val graph = LocalAppUiGraph.current
    val screens = remember(graph) { settingsScreens(graph) }
    return screens.flatMap { screen ->
        val screenTitle = stringResource(screen.getTitleRes())
        screen.getPreferences().storedSettings(graph.profileStore).mapNotNull { (key, title) ->
            val mark = PreferenceScopes.markOf(key)?.takeIf { it.area == area } ?: return@mapNotNull null
            AreaSetting(key, title, screenTitle, mark)
        }
    }.distinctBy { it.key }
}

/**
 * Whether the area has a page listing its settings. Extension settings are each source's own, which no Settings screen
 * lists, so they sync or stay as a whole; an area that follows a data group goes with it.
 */
internal val SettingsArea.hasPage: Boolean
    get() = this in SettingsArea.switchable && this != SettingsArea.EXTENSION_SETTINGS

/**
 * The Settings area most of these rows' settings sync with, such as Readers for a reader's settings; null for a screen
 * whose settings stay on the device.
 */
internal fun List<Preference>.syncArea(profileStore: ProfileStore): SettingsArea? = storedSettings(profileStore)
    .mapNotNull { (key, _) -> PreferenceScopes.markOf(key)?.area }
    .groupingBy { it }
    .eachCount()
    .maxByOrNull { it.value }
    ?.key

/** The key and title of every row that stores its value in a preference. */
private fun List<Preference>.storedSettings(profileStore: ProfileStore): List<Pair<String, String>> =
    flatMap { preference ->
        when (preference) {
            is Preference.PreferenceGroup -> preference.preferenceItems
            is Preference.PreferenceItem<*, *> -> listOf(preference)
        }
    }.mapNotNull { item -> item.storedPreference?.let { profileStore.preferenceKey(it) to item.title } }
