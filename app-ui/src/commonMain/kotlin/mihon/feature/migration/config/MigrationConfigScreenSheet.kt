package mihon.feature.migration.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import eu.kanade.domain.source.service.SourcePreferences
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.feature.migration.options.getLabel
import mihon.feature.migration.session.model.SourceMigrationDiscoveryDepth
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.Warning
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.field.TextField
import mihon.presentation.kit.list.ChipsRow
import mihon.presentation.kit.list.ControlRow
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.list.ToggleRow
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.toggle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun MigrationConfigScreenSheet(
    preferences: SourcePreferences,
    showSearchOptions: Boolean = true,
    showInitialAdvancedSearchOption: Boolean = false,
    onDismissRequest: () -> Unit,
    onStartMigration: (
        extraSearchQuery: String?,
        selectedOptions: Set<EntryMigrationOption>,
        initialDiscoveryDepth: SourceMigrationDiscoveryDepth,
    ) -> Unit,
) {
    var extraSearchQuery by rememberSaveable { mutableStateOf("") }
    var initialAdvancedSearch by rememberSaveable { mutableStateOf(false) }
    var carriedOptions by rememberSaveable(stateSaver = CarriedOptionsSaver) { mutableStateOf(CARRIED_OPTIONS) }
    Sheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                ChipsRow(title = stringResource(MR.strings.migrationConfigScreen_dataToMigrateHeader)) {
                    CARRIED_OPTIONS.fastForEach { option ->
                        FilterChip(
                            label = stringResource(option.getLabel()),
                            selected = option in carriedOptions,
                            onClick = {
                                carriedOptions = if (option in carriedOptions) {
                                    carriedOptions - option
                                } else {
                                    carriedOptions + option
                                }
                            },
                        )
                    }
                }
                RowGroup {
                    row {
                        PreferenceToggleRow(
                            title = stringResource(MR.strings.migrationConfigScreen_removeDownloadsTitle),
                            subtitle = null,
                            preference = preferences.migrationRemoveDownloads,
                        )
                    }
                    if (showInitialAdvancedSearchOption) {
                        row {
                            ToggleRow(
                                title = stringResource(MR.strings.migrationConfigScreen_deepSearchModeTitle),
                                subtitle = stringResource(MR.strings.migrationConfigScreen_deepSearchModeSubtitle),
                                checked = initialAdvancedSearch,
                                onCheckedChange = { initialAdvancedSearch = it },
                            )
                        }
                    }
                }
                if (showSearchOptions) {
                    RowGroupGap()
                    RowGroup {
                        row {
                            ControlRow {
                                TextField(
                                    value = extraSearchQuery,
                                    onValueChange = { extraSearchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = stringResource(MR.strings.migrationConfigScreen_additionalSearchQueryLabel),
                                    supportingText = stringResource(
                                        MR.strings.migrationConfigScreen_additionalSearchQuerySupportingText,
                                    ),
                                )
                            }
                        }
                        row {
                            PreferenceToggleRow(
                                title = stringResource(MR.strings.migrationConfigScreen_hideUnmatchedTitle),
                                subtitle = null,
                                preference = preferences.migrationHideUnmatched,
                            )
                        }
                        row {
                            PreferenceToggleRow(
                                title = stringResource(MR.strings.migrationConfigScreen_hideWithoutUpdatesTitle),
                                subtitle = stringResource(MR.strings.migrationConfigScreen_hideWithoutUpdatesSubtitle),
                                preference = preferences.migrationHideWithoutUpdates,
                            )
                        }
                    }
                    RowGroupNote(
                        text = stringResource(MR.strings.migrationConfigScreen_enhancedOptionsWarning),
                        tone = ContentTone.Error,
                    )
                    RowGroup {
                        row {
                            PreferenceToggleRow(
                                title = stringResource(MR.strings.migrationConfigScreen_deepSearchModeTitle),
                                subtitle = stringResource(MR.strings.migrationConfigScreen_deepSearchModeSubtitle),
                                preference = preferences.migrationDeepSearchMode,
                            )
                        }
                        row {
                            PreferenceToggleRow(
                                title = stringResource(MR.strings.migrationConfigScreen_prioritizeByChaptersTitle),
                                subtitle = stringResource(
                                    MR.strings.migrationConfigScreen_prioritizeByChaptersSubtitle,
                                ),
                                preference = preferences.migrationPrioritizeByChapters,
                            )
                        }
                    }
                }
            }
            Button(
                label = stringResource(MR.strings.migrationConfigScreen_continueButtonText),
                onClick = {
                    val cleanedExtraSearchQuery = extraSearchQuery.trim().ifBlank { null }
                    onStartMigration(
                        cleanedExtraSearchQuery,
                        if (preferences.migrationRemoveDownloads.get()) {
                            carriedOptions.toSet() + EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS
                        } else {
                            carriedOptions.toSet()
                        },
                        if (initialAdvancedSearch) {
                            SourceMigrationDiscoveryDepth.BROAD
                        } else {
                            SourceMigrationDiscoveryDepth.STANDARD
                        },
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(KatariTheme.spacing.m),
            )
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String?,
    preference: Preference<Boolean>,
) {
    ToggleRow(
        title = title,
        subtitle = subtitle,
        checked = preference.collectAsState().value,
        onCheckedChange = { preference.toggle() },
    )
}

/** What a migration can carry over, all chosen when the sheet opens. */
private val CARRIED_OPTIONS = EntryMigrationOption.entries - EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS

private val CarriedOptionsSaver: Saver<List<EntryMigrationOption>, Any> = listSaver(
    save = { options -> options.map { it.name } },
    restore = { names -> names.map(EntryMigrationOption::valueOf) },
)
