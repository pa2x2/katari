package app.katari.sync.ui.arrival.change

import androidx.compose.runtime.Composable
import app.katari.sync.client.arrival.ArrivedSetting
import app.katari.sync.client.setting.scope.MenuSettings
import app.katari.sync.ui.arrival.change.menu.chapterListDefaultChanges
import app.katari.sync.ui.arrival.change.menu.libraryFilterChanges
import app.katari.sync.ui.arrival.change.menu.librarySortChanges
import app.katari.sync.ui.arrival.change.menu.savedFeedChanges
import app.katari.sync.ui.arrival.change.menu.sourcesChanges
import app.katari.sync.ui.arrival.change.menu.updatesFilterChanges
import eu.kanade.presentation.more.settings.Preference
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What other devices changed in [settings], one setting or the settings of one menu, as parts a row tells. A setting
 * is told with its Settings row ([rows]), a menu by the labels of its own menu. Empty when the settings read as they did
 * before; null when what changed is unknown: a change noted before the app kept the value it arrived with, or a setting
 * none of these can tell.
 */
@Composable
internal fun arrivedChanges(
    settings: List<ArrivedSetting>,
    rows: Map<String, Preference.PreferenceItem<*, *>>,
    lookups: ChangeLookups,
): List<ChangePart>? {
    if (settings.any { it.change == null }) return null
    val changes = settings.mapNotNull { setting ->
        setting.change?.let { KeyChange(setting.profileId, setting.key, it) }
    }
    return when (settings.first().menu) {
        null -> {
            val change = changes.single()
            val row = rows[change.key]
            val named = lookups.namedSetting(change.key)
            // A row of its own make, such as a chooser of pictures, is told by the setting's name instead
            row?.let { settingRowChanges(it, change.change) } ?: named?.changes(change)
        }
        MenuSettings.LIBRARY_SORT -> librarySortChanges(changes)
        MenuSettings.LIBRARY_FILTERS -> libraryFilterChanges(changes)
        MenuSettings.CHAPTER_LIST_DEFAULTS -> chapterListDefaultChanges(changes)
        MenuSettings.UPDATES_FILTERS -> updatesFilterChanges(changes, lookups)
        MenuSettings.SOURCES -> sourcesChanges(changes, lookups)
        MenuSettings.SAVED_FEEDS -> savedFeedChanges(changes, lookups)
    }
}

/**
 * The parts on one line, each after its name unless it reads alone or the row it is on already has that name
 * ([rowTitle]): "Filtered to Unread · Sort: By source → By upload date".
 */
@Composable
internal fun List<ChangePart>.line(rowTitle: String?): String = map { part ->
    if (part.readsAlone || part.name == rowTitle) {
        part.text
    } else {
        stringResource(MR.strings.sync_arrived_change_named, part.name, part.text)
    }
}.joinToString(" · ")
