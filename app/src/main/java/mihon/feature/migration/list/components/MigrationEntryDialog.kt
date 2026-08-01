package mihon.feature.migration.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import mihon.feature.migration.list.models.MigrationMergeImpactSummary
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MigrationEntryDialog(
    onDismissRequest: () -> Unit,
    copy: Boolean,
    totalCount: Int,
    skippedCount: Int,
    mergeImpact: MigrationMergeImpactSummary,
    onMigrate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = pluralStringResource(
                    resource = if (copy) {
                        MR.plurals.migrationListScreen_migrateDialog_copyTitle
                    } else {
                        MR.plurals.migrationListScreen_migrateDialog_migrateTitle
                    },
                    count = totalCount,
                    totalCount,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                if (skippedCount > 0) {
                    Text(
                        text = pluralStringResource(
                            resource = MR.plurals.migrationListScreen_migrateDialog_skipText,
                            count = skippedCount,
                            skippedCount,
                        ),
                    )
                }
                if (copy && mergeImpact.hasAnyMergedEntries) {
                    Text(stringResource(MR.strings.migrationListScreen_migrateDialog_copyMergedExplanation))
                }
                if (!copy && mergeImpact.hasMergedRoots) {
                    Text(stringResource(MR.strings.migrationListScreen_migrateDialog_replaceRootExplanation))
                }
                if (!copy && mergeImpact.hasMergedMembers) {
                    Text(stringResource(MR.strings.migrationListScreen_migrateDialog_replaceMemberExplanation))
                }
                if (!copy && mergeImpact.hasSameGroupTargets) {
                    Text(stringResource(MR.strings.migrationListScreen_migrateDialog_sameGroupTargetExplanation))
                }
                if (!copy && mergeImpact.hasOtherGroupTargets) {
                    Text(stringResource(MR.strings.migrationListScreen_migrateDialog_otherGroupTargetExplanation))
                }
                if (!copy && mergeImpact.hasStandaloneSourcesWithMergedTargets) {
                    Text(stringResource(MR.strings.migrationListScreen_migrateDialog_standaloneToMergedExplanation))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onMigrate) {
                Text(
                    text = stringResource(
                        resource = if (copy) {
                            MR.strings.migrationListScreen_migrateDialog_copyLabel
                        } else {
                            MR.strings.migrationListScreen_migrateDialog_migrateLabel
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.migrationListScreen_migrateDialog_cancelLabel))
            }
        },
    )
}
