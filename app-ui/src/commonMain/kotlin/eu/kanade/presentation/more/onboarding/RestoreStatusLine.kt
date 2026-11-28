package eu.kanade.presentation.more.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.feedback.StatusLine
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreStatus.State
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** The line setup's pages show about the restore it started (D120): how far it has got, then how it ended. */
@Composable
internal fun RestoreStatusLine() {
    val state by LocalAppUiGraph.current.backupRestoreStatus.state.collectAsStateWithLifecycle()
    when (val restore = state) {
        null -> Unit
        is State.Running -> StatusLine(
            onClick = null,
            progress = { if (restore.total == 0) 0f else restore.progress.toFloat() / restore.total },
        ) {
            Text(stringResource(MR.strings.onboarding_restore_running, restore.progress, restore.total))
        }
        is State.Restored -> StatusLine(onClick = null) {
            Text(
                if (restore.errorCount == 0) {
                    stringResource(MR.strings.onboarding_restore_done)
                } else {
                    pluralStringResource(
                        MR.plurals.onboarding_restore_done_errors,
                        restore.errorCount,
                        restore.errorCount,
                    )
                },
            )
        }
        State.Stopped -> StatusLine(onClick = null) { Text(stringResource(MR.strings.onboarding_restore_stopped)) }
    }
}
