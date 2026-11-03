package mihon.feature.migration.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import mihon.feature.migration.list.models.MigrationMergeImpactSummary
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
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
    Dialog(
        onDismissRequest = onDismissRequest,
        title = pluralStringResource(
            resource = if (copy) {
                MR.plurals.migrationListScreen_migrateDialog_copyTitle
            } else {
                MR.plurals.migrationListScreen_migrateDialog_migrateTitle
            },
            count = totalCount,
            totalCount,
        ),
        confirm = DialogAction(
            label = stringResource(
                resource = if (copy) {
                    MR.strings.migrationListScreen_migrateDialog_copyLabel
                } else {
                    MR.strings.migrationListScreen_migrateDialog_migrateLabel
                },
            ),
            onClick = onMigrate,
        ),
        dismiss = DialogAction(
            label = stringResource(MR.strings.migrationListScreen_migrateDialog_cancelLabel),
            onClick = onDismissRequest,
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
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
    }
}
