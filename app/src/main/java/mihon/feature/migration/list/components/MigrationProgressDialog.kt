package mihon.feature.migration.list.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.dialog.ProgressDialog
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MigrationProgressDialog(
    progress: Float,
    exitMigration: () -> Unit,
) {
    val progressAnimated by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
        label = "migration_progress",
    )
    ProgressDialog(
        message = stringResource(MR.strings.sourceMigration_migrating),
        progress = { progressAnimated },
        cancel = DialogAction(
            label = stringResource(MR.strings.migrationListScreen_progressDialog_cancelLabel),
            onClick = exitMigration,
        ),
    )
}
