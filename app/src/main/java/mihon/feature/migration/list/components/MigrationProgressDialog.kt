package mihon.feature.migration.list.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.DialogProperties
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.progress.LinearProgressIndicator
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MigrationProgressDialog(
    progress: Float,
    exitMigration: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        title = null,
        confirm = DialogAction(
            label = stringResource(MR.strings.migrationListScreen_progressDialog_cancelLabel),
            onClick = exitMigration,
        ),
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        if (!progress.isNaN()) {
            val progressAnimated by animateFloatAsState(
                targetValue = progress,
                animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
                label = "migration_progress",
            )
            LinearProgressIndicator(progress = { progressAnimated })
        }
    }
}
