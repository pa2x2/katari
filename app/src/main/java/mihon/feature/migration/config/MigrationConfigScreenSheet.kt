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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import eu.kanade.core.util.fastFilterNot
import eu.kanade.domain.source.service.SourcePreferences
import mihon.domain.migration.models.MigrationFlag
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.feature.migration.options.getLabel
import mihon.feature.migration.options.toEntryMigrationOptions
import mihon.feature.migration.session.model.SourceMigrationDiscoveryDepth
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.Warning
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.field.TextField
import mihon.presentation.kit.list.ChipsRow
import mihon.presentation.kit.list.ToggleRow
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.getAndSet
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
    val migrationFlags by preferences.migrationFlags.collectAsState()
    Sheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                ChipsRow(title = stringResource(MR.strings.migrationConfigScreen_dataToMigrateHeader)) {
                    MigrationFlag.entries
                        .fastFilterNot { it == MigrationFlag.REMOVE_DOWNLOAD }
                        .fastForEach { flag ->
                            FilterChip(
                                label = stringResource(flag.getLabel()),
                                selected = flag in migrationFlags,
                                onClick = {
                                    preferences.migrationFlags.getAndSet { currentFlags ->
                                        if (flag in currentFlags) {
                                            currentFlags - flag
                                        } else {
                                            currentFlags + flag
                                        }
                                    }
                                },
                            )
                        }
                }
                val removeDownloads = MigrationFlag.REMOVE_DOWNLOAD in migrationFlags
                ToggleRow(
                    title = stringResource(MR.strings.migrationConfigScreen_removeDownloadsTitle),
                    checked = removeDownloads,
                    onCheckedChange = { remove ->
                        preferences.migrationFlags.getAndSet {
                            if (remove) it + MigrationFlag.REMOVE_DOWNLOAD else it - MigrationFlag.REMOVE_DOWNLOAD
                        }
                    },
                )
                if (showInitialAdvancedSearchOption) {
                    HorizontalDivider()
                    ToggleRow(
                        title = stringResource(MR.strings.migrationConfigScreen_deepSearchModeTitle),
                        subtitle = stringResource(MR.strings.migrationConfigScreen_deepSearchModeSubtitle),
                        checked = initialAdvancedSearch,
                        onCheckedChange = { initialAdvancedSearch = it },
                    )
                }
                if (showSearchOptions) {
                    HorizontalDivider()
                    TextField(
                        value = extraSearchQuery,
                        onValueChange = { extraSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.s),
                        label = stringResource(MR.strings.migrationConfigScreen_additionalSearchQueryLabel),
                        supportingText = stringResource(
                            MR.strings.migrationConfigScreen_additionalSearchQuerySupportingText,
                        ),
                    )
                    PreferenceToggleRow(
                        title = stringResource(MR.strings.migrationConfigScreen_hideUnmatchedTitle),
                        subtitle = null,
                        preference = preferences.migrationHideUnmatched,
                    )
                    PreferenceToggleRow(
                        title = stringResource(MR.strings.migrationConfigScreen_hideWithoutUpdatesTitle),
                        subtitle = stringResource(MR.strings.migrationConfigScreen_hideWithoutUpdatesSubtitle),
                        preference = preferences.migrationHideWithoutUpdates,
                    )
                    HorizontalDivider()
                    EnhancedOptionsWarning(stringResource(MR.strings.migrationConfigScreen_enhancedOptionsWarning))
                    PreferenceToggleRow(
                        title = stringResource(MR.strings.migrationConfigScreen_deepSearchModeTitle),
                        subtitle = stringResource(MR.strings.migrationConfigScreen_deepSearchModeSubtitle),
                        preference = preferences.migrationDeepSearchMode,
                    )
                    PreferenceToggleRow(
                        title = stringResource(MR.strings.migrationConfigScreen_prioritizeByChaptersTitle),
                        subtitle = stringResource(MR.strings.migrationConfigScreen_prioritizeByChaptersSubtitle),
                        preference = preferences.migrationPrioritizeByChapters,
                    )
                }
            }
            HorizontalDivider()
            Button(
                label = stringResource(MR.strings.migrationConfigScreen_continueButtonText),
                onClick = {
                    val cleanedExtraSearchQuery = extraSearchQuery.trim().ifBlank { null }
                    onStartMigration(
                        cleanedExtraSearchQuery,
                        migrationFlags.toEntryMigrationOptions(),
                        if (initialAdvancedSearch) {
                            SourceMigrationDiscoveryDepth.BROAD
                        } else {
                            SourceMigrationDiscoveryDepth.STANDARD
                        },
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.s),
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

@Composable
private fun EnhancedOptionsWarning(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.m),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = MaterialSymbols.RoundedFilled.Warning, contentDescription = null, tone = ContentTone.Active)
        Text(text = text, role = TextRole.Body, tone = ContentTone.Error)
    }
}
