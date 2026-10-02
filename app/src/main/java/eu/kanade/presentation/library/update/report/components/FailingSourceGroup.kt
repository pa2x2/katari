package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.presentation.library.update.report.LibraryUpdateReportActions
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.feature.library.update.report.LibraryUpdateReport
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One row for a source whose checks mostly failed the same way, expanding to the entries it stands for.
 *
 * A source failing once is usually down for a while, so its entries can be retried together; one that keeps failing
 * needs its entries moved elsewhere. Pausing the source fits both.
 *
 * @param name null when the source isn't installed and its name was never recorded.
 * @param migrations the group's entries that can be migrated.
 */
@Composable
internal fun FailingSourceGroup(
    source: LibraryUpdateReport.FailingSource,
    name: String?,
    isPaused: Boolean,
    migrations: List<EntryMigrationSubject>,
    actions: LibraryUpdateReportActions,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        ReportRow(
            entry = null,
            title = pluralStringResource(
                MR.plurals.library_update_report_source_entries,
                source.items.size,
                name ?: stringResource(MR.strings.library_update_report_unknown_source),
                source.items.size,
            ),
            subtitle = source.error ?: stringResource(MR.strings.library_update_report_unknown_error),
            isError = true,
            onClick = { expanded = !expanded },
            trailing = {
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                )
            },
        )
        if (expanded) {
            source.items.forEach { item ->
                ReportRow(
                    entry = item.entry,
                    title = item.entry.title,
                    subtitle = null,
                    modifier = Modifier.padding(start = MaterialTheme.padding.medium),
                    onClick = { actions.onClickEntry(item.entry) },
                )
            }
        }
        ActionChips(start = SOURCE_ACTIONS_START) {
            if (!source.isFailingRepeatedly) {
                AssistChip(
                    onClick = { actions.onRetry(source.items.map { it.entry }) },
                    label = {
                        Text(
                            pluralStringResource(
                                MR.plurals.library_update_report_retry_entries,
                                source.items.size,
                                source.items.size,
                            ),
                        )
                    },
                )
            } else if (migrations.isNotEmpty()) {
                AssistChip(
                    onClick = { actions.onMigrate(migrations) },
                    label = {
                        Text(
                            pluralStringResource(
                                MR.plurals.library_update_report_migrate_entries,
                                migrations.size,
                                migrations.size,
                            ),
                        )
                    },
                )
            }
            PauseChip(
                paused = isPaused,
                label = stringResource(MR.strings.library_update_report_pause_source),
                onPausedChange = { actions.onSetSourcePaused(source.sourceId, it) },
            )
        }
    }
}
