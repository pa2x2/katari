package mihon.feature.migration.review.components

import androidx.compose.runtime.Composable
import mihon.presentation.kit.dialog.ConfirmDialog
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun SourceMigrationDiscardDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(MR.strings.sourceMigrationReview_discardTitle),
        text = stringResource(MR.strings.sourceMigrationReview_discardMessage),
        confirmLabel = stringResource(MR.strings.sourceMigrationReview_discardConfirm),
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest,
        destructive = true,
    )
}
