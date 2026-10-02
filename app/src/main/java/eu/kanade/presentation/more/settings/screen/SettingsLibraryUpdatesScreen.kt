package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.library.update.libraryUpdateRunSummaryText
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.libraryupdates.CategoryUpdateRulesScreen
import eu.kanade.presentation.more.settings.screen.libraryupdates.LibraryUpdatesSettingsScreenModel
import eu.kanade.presentation.more.settings.screen.libraryupdates.libraryUpdateIntervalLabel
import eu.kanade.presentation.more.settings.screen.libraryupdates.libraryUpdateIntervals
import eu.kanade.presentation.more.settings.screen.libraryupdates.titleRes
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportScreen
import mihon.feature.library.update.planning.LibraryUpdatePreview
import mihon.feature.library.update.planning.LibraryUpdateSettings
import mihon.feature.library.update.planning.LibraryUpdateSkipRule
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.runtime.collectAsState as collectFlowAsState

/**
 * What library updates check and when. Every setting here belongs to the active profile.
 *
 * Categories, sources and types are switches that take their entries out of updates; the skip rules leave out entries
 * that probably have nothing new. The entry screen can override all of it for one entry.
 */
object SettingsLibraryUpdatesScreen : SearchableSettings {
    private fun readResolve(): Any = SettingsLibraryUpdatesScreen

    private const val COLLAPSED_ROWS = 5

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.pref_category_library_updates

    @Composable
    override fun getPreferences(): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
        val screenModel = rememberScreenModel { LibraryUpdatesSettingsScreenModel() }
        val state by screenModel.state.collectFlowAsState()

        return listOfNotNull(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.library_updates_last_update),
                subtitle = state.lastRun?.let { libraryUpdateRunSummaryText(it) }
                    ?: stringResource(MR.strings.library_updates_last_update_none),
                isProfileSpecific = true,
                onClick = { navigator.push(LibraryUpdateReportScreen()) }.takeIf { state.lastRun != null },
            ),
            Preference.PreferenceItem.CustomPreference(
                title = stringResource(MR.strings.library_updates_next_automatic),
                isProfileSpecific = true,
            ) {
                NextUpdateCard(nextRunAt = state.nextRunAt, preview = state.preview)
            },
            getScheduleGroup(screenModel, state, libraryPreferences),
            getCategoriesGroup(state, screenModel, onOpenCategory = { navigator.push(CategoryUpdateRulesScreen(it)) })
                .takeIf { state.hasUserCategories },
            getSourcesGroup(state, screenModel).takeIf { state.sources.isNotEmpty() },
            getTypesGroup(state, screenModel).takeIf { state.types.size > 1 },
            getSkipRulesGroup(state, screenModel, libraryPreferences),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.library_updates_also),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = libraryPreferences.autoUpdateMetadata,
                        title = stringResource(MR.strings.pref_library_update_refresh_metadata),
                        subtitle = stringResource(MR.strings.pref_library_update_refresh_metadata_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = libraryPreferences.newShowUpdatesCount,
                        title = stringResource(MR.strings.pref_library_update_show_tab_badge),
                    ),
                ),
            ),
        )
    }

    @Composable
    private fun NextUpdateCard(nextRunAt: Long?, preview: LibraryUpdatePreview?) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (nextRunAt != null) {
                        "${stringResource(MR.strings.library_updates_next_automatic)} · " +
                            relativeTimeSpanString(nextRunAt)
                    } else {
                        stringResource(MR.strings.library_updates_automatic_off)
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                if (preview != null) {
                    Text(
                        text = pluralStringResource(
                            if (nextRunAt != null) {
                                MR.plurals.library_updates_checks_of
                            } else {
                                MR.plurals.library_updates_manual_checks_of
                            },
                            preview.librarySize,
                            preview.checks,
                            preview.librarySize,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    @Composable
    private fun getScheduleGroup(
        screenModel: LibraryUpdatesSettingsScreenModel,
        state: LibraryUpdatesSettingsScreenModel.State,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val interval by libraryPreferences.autoUpdateInterval.collectAsState()
        val schedulePeriod = LibraryUpdateSettings.schedulePeriodHours(interval, state.categories.map { it.rules })
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.library_updates_schedule),
            preferenceItems = listOfNotNull(
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.autoUpdateInterval,
                    entries = mapOf(0 to stringResource(MR.strings.update_never)) + libraryUpdateIntervals(),
                    title = stringResource(MR.strings.pref_library_update_interval),
                    onValueChanged = {
                        screenModel.setInterval(it)
                        true
                    },
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.autoUpdateDeviceRestrictions,
                    entries = mapOf(
                        DEVICE_ONLY_ON_WIFI to stringResource(MR.strings.connected_to_wifi),
                        DEVICE_NETWORK_NOT_METERED to stringResource(MR.strings.network_not_metered),
                        DEVICE_CHARGING to stringResource(MR.strings.charging),
                    ),
                    title = stringResource(MR.strings.pref_library_update_restriction),
                    subtitle = stringResource(MR.strings.restrictions),
                    onValueChanged = {
                        screenModel.rescheduleAfterDeviceConditionsChange()
                        true
                    },
                ).takeIf { schedulePeriod != null },
            ),
        )
    }

    @Composable
    private fun getCategoriesGroup(
        state: LibraryUpdatesSettingsScreenModel.State,
        screenModel: LibraryUpdatesSettingsScreenModel,
        onOpenCategory: (Long) -> Unit,
    ): Preference.PreferenceGroup {
        var expanded by rememberSaveable { mutableStateOf(false) }
        val rows = if (expanded) state.categories else state.categories.take(COLLAPSED_ROWS)
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.categories),
            preferenceItems = rows.map { row ->
                val overrides = row.rules.overrides
                Preference.PreferenceItem.TextPreference(
                    title = row.category.visualName,
                    subtitle = buildList {
                        add(pluralStringResource(MR.plurals.library_updates_entries, row.entryCount, row.entryCount))
                        if (!row.rules.autoUpdate) {
                            add(stringResource(MR.strings.library_updates_not_checked))
                        } else if (overrides != null) {
                            overrides.intervalHours?.let { add(libraryUpdateIntervalLabel(it)) }
                            add(stringResource(MR.strings.library_updates_own_skip_rules))
                        }
                    }
                        .joinToString(" · "),
                    isProfileSpecific = true,
                    widget = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (overrides != null) {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 12.dp),
                                )
                            }
                            Switch(
                                checked = row.rules.autoUpdate,
                                onCheckedChange = { screenModel.setCategoryChecked(row.category.id, it) },
                            )
                        }
                    },
                    onClick = { onOpenCategory(row.category.id) },
                )
            } + listOfNotNull(
                showAllToggle(expanded, onToggle = { expanded = !expanded })
                    .takeIf { state.categories.size > COLLAPSED_ROWS },
            ),
        )
    }

    @Composable
    private fun getSourcesGroup(
        state: LibraryUpdatesSettingsScreenModel.State,
        screenModel: LibraryUpdatesSettingsScreenModel,
    ): Preference.PreferenceGroup {
        var expanded by rememberSaveable { mutableStateOf(false) }
        val off = state.sources.count { !it.checked }
        val total = state.sources.size
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_sources),
            preferenceItems = if (expanded) {
                state.sources.map { row ->
                    Preference.PreferenceItem.TextPreference(
                        title = row.name,
                        subtitle = pluralStringResource(
                            MR.plurals.library_updates_entries,
                            row.entryCount,
                            row.entryCount,
                        ),
                        isProfileSpecific = true,
                        widget = { Switch(checked = row.checked, onCheckedChange = null) },
                        onClick = { screenModel.setSourceChecked(row.id, !row.checked) },
                    )
                } + showAllToggle(expanded = true, onToggle = { expanded = false })
            } else {
                listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = if (off == 0) {
                            pluralStringResource(MR.plurals.library_updates_sources_all_checked, total, total)
                        } else {
                            pluralStringResource(MR.plurals.library_updates_sources_some_off, total, off, total)
                        },
                        subtitle = stringResource(MR.strings.library_updates_show_all),
                        isProfileSpecific = true,
                        onClick = { expanded = true },
                    ),
                )
            },
        )
    }

    @Composable
    private fun getTypesGroup(
        state: LibraryUpdatesSettingsScreenModel.State,
        screenModel: LibraryUpdatesSettingsScreenModel,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.library_updates_types),
            preferenceItems = state.types.map { row ->
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(row.type.entryTypePresentation().displayNameLabel),
                    subtitle = pluralStringResource(MR.plurals.library_updates_entries, row.entryCount, row.entryCount),
                    isProfileSpecific = true,
                    widget = { Switch(checked = row.checked, onCheckedChange = null) },
                    onClick = { screenModel.setTypeChecked(row.type, !row.checked) },
                )
            },
        )
    }

    @Composable
    private fun getSkipRulesGroup(
        state: LibraryUpdatesSettingsScreenModel.State,
        screenModel: LibraryUpdatesSettingsScreenModel,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val values by libraryPreferences.updateSkipRules.collectAsState()
        val rules = LibraryPreferences.skipRulesOf(values)
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.library_updates_skip_when),
            preferenceItems = LibraryUpdateSkipRule.entries.map { rule ->
                val on = rule.isOn(rules)
                val hits = state.preview?.skipRuleHits?.get(rule)
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(rule.titleRes),
                    subtitle = hits?.let {
                        if (rule == LibraryUpdateSkipRule.OUTSIDE_RELEASE_PERIOD) {
                            pluralStringResource(MR.plurals.library_updates_defers, it, it)
                        } else {
                            pluralStringResource(MR.plurals.library_updates_entries, it, it)
                        }
                    },
                    isProfileSpecific = true,
                    widget = { Switch(checked = on, onCheckedChange = null) },
                    onClick = { screenModel.setSkipRule(rule, !on) },
                )
            },
        )
    }

    @Composable
    private fun showAllToggle(expanded: Boolean, onToggle: () -> Unit) = Preference.PreferenceItem.TextPreference(
        title = stringResource(
            if (expanded) MR.strings.library_updates_show_fewer else MR.strings.library_updates_show_all,
        ),
        isProfileSpecific = true,
        onClick = onToggle,
    )
}
