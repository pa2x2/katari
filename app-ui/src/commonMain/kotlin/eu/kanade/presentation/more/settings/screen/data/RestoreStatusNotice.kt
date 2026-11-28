package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CheckCircle
import app.katari.icons.materialsymbols.rounded.SettingsBackupRestore
import app.katari.presentation.kit.surface.NoticeCard
import app.katari.presentation.kit.surface.NoticeTone
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreStatus.State
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * How the restore the person started is going, then how it ended: on setup's pages after it starts (D120), and at the
 * top of the Library, where it is the only sign of a restore when notifications don't show (D142). [dismissible] adds
 * OK to its end, which puts the notice away until the next restore.
 */
@Composable
internal fun RestoreStatusNotice(dismissible: Boolean, modifier: Modifier = Modifier) {
    val status = LocalAppUiGraph.current.backupRestoreStatus
    val state by status.state.collectAsStateWithLifecycle()
    val restore = state ?: return
    val (text, tone) = when (restore) {
        is State.Running -> stringResource(MR.strings.restore_status_running, restore.progress, restore.total) to
            NoticeTone.Info
        is State.Restored -> if (restore.errorCount == 0) {
            stringResource(MR.strings.restore_status_done) to NoticeTone.Info
        } else {
            pluralStringResource(MR.plurals.restore_status_done_errors, restore.errorCount, restore.errorCount) to
                NoticeTone.Warning
        }
        State.Stopped -> stringResource(MR.strings.restore_status_stopped) to NoticeTone.Warning
    }
    val ended = restore !is State.Running
    NoticeCard(
        text = text,
        tone = tone,
        modifier = modifier,
        icon = when {
            restore is State.Running -> MaterialSymbols.Rounded.SettingsBackupRestore
            tone == NoticeTone.Info -> MaterialSymbols.Rounded.CheckCircle
            else -> null
        },
        actionLabel = stringResource(MR.strings.action_ok).takeIf { dismissible && ended },
        onAction = status::dismiss,
        progress = (restore as? State.Running)?.let { running ->
            { if (running.total == 0) 0f else running.progress.toFloat() / running.total }
        },
    )
}
