package eu.kanade.presentation.more.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.SettingsBackupRestore
import app.katari.presentation.kit.surface.NoticeCard
import app.katari.presentation.kit.surface.NoticeTone
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreStatus.State
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** How the restore setup started is going, on every setup page after it starts (D120), then how it ended. */
@Composable
internal fun RestoreStatusNotice() {
    val state by LocalAppUiGraph.current.backupRestoreStatus.state.collectAsStateWithLifecycle()
    when (val restore = state) {
        null -> Unit
        is State.Running -> NoticeCard(
            text = stringResource(MR.strings.onboarding_restore_running, restore.progress, restore.total),
            tone = NoticeTone.Info,
            icon = MaterialSymbols.Rounded.SettingsBackupRestore,
            progress = { if (restore.total == 0) 0f else restore.progress.toFloat() / restore.total },
        )
        is State.Restored -> if (restore.errorCount == 0) {
            NoticeCard(text = stringResource(MR.strings.onboarding_restore_done), tone = NoticeTone.Success)
        } else {
            NoticeCard(
                text = pluralStringResource(
                    MR.plurals.onboarding_restore_done_errors,
                    restore.errorCount,
                    restore.errorCount,
                ),
                tone = NoticeTone.Warning,
            )
        }
        State.Stopped -> NoticeCard(
            text = stringResource(MR.strings.onboarding_restore_stopped),
            tone = NoticeTone.Warning,
        )
    }
}
