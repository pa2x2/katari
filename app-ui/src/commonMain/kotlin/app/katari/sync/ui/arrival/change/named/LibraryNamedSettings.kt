package app.katari.sync.ui.arrival.change.named

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.katari.library.update.planning.LibraryUpdateSkipRule
import app.katari.sync.ui.arrival.change.listChange
import app.katari.sync.ui.arrival.change.setChanges
import app.katari.sync.ui.arrival.change.switchChange
import app.katari.ui.di.LocalAppUiGraph
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.library.settings.labelRes
import eu.kanade.presentation.more.settings.screen.libraryupdates.titleRes
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.upcoming.service.UpcomingPreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The library's settings changed on rows of their own make: its display mode, Library updates' skip rules, paused
 * sources and types, duplicate detection's title exclusions, and the categories Upcoming shows or leaves out.
 */
internal val libraryNamedSettings = listOf(
    valueSetting(MR.strings.action_display_mode, {
        LibraryPreferences(it).displayMode
    }) { stringResource(it.labelRes) },
    namedSetting(MR.strings.library_updates_skip_when, { LibraryPreferences(it).updateSkipRules }) { _, value, _ ->
        val before = LibraryPreferences.skipRulesOf(value.before)
        val after = LibraryPreferences.skipRulesOf(value.after)
        LibraryUpdateSkipRule.entries.mapNotNull { rule ->
            switchChange(stringResource(rule.titleRes), rule.isOn(before), rule.isOn(after))
        }
    },
    namedSetting(MR.strings.sync_setting_paused_sources, { LibraryPreferences(it).updatePausedSources }) {
            name,
            value,
            _,
        ->
        val lookups = LocalAppUiGraph.current.changeLookups
        val before = value.before.mapTo(mutableSetOf()) { it.sourceId }
        val after = value.after.mapTo(mutableSetOf()) { it.sourceId }
        listOfNotNull(
            listChange(name, MR.strings.sync_arrived_paused, (after - before).map(lookups::sourceName)),
            listChange(name, MR.strings.sync_arrived_resumed, (before - after).map(lookups::sourceName)),
        )
    },
    namedSetting(MR.strings.sync_setting_update_types, { LibraryPreferences(it).updateExcludedEntryTypes }) {
            name,
            value,
            _,
        ->
        setChanges(
            name,
            value.before,
            value.after,
            { stringResource(it.entryTypePresentation().displayNameLabel) },
            added = MR.strings.sync_arrived_turned_off_for,
            removed = MR.strings.sync_arrived_turned_on_for,
            readsAlone = true,
        )
    },
    orderSetting(MR.strings.pref_duplicate_detection_title_exclusions, {
        DuplicatePreferences(it).titleExclusionPatterns
    }) { it },
    categoriesSetting(MR.strings.sync_setting_upcoming_included_categories) {
        UpcomingPreferences(it).filterIncludedCategories
    },
    categoriesSetting(MR.strings.sync_setting_upcoming_excluded_categories) {
        UpcomingPreferences(it).filterExcludedCategories
    },
)

/** Categories by id, named as the profile that holds the setting names them. */
private fun categoriesSetting(
    name: StringResource,
    preference: (PreferenceStore) -> Preference<List<Long>>,
) = namedSetting(name, preference) { text, value, profileId ->
    val lookups = LocalAppUiGraph.current.changeLookups
    val categories by remember(lookups, profileId) { lookups.categories(profileId) }.collectAsState(emptyList())
    val names = categories.associate { it.id to it.visualName }
    setChanges(text, value.before.toSet(), value.after.toSet(), { names[it] ?: it.toString() })
}
