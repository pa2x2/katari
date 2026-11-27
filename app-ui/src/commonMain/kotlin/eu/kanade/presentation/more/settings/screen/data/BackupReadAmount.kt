package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.data.backup.storage.BackupReadProgress
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.fileSize
import tachiyomi.presentation.core.i18n.stringResource

/** How much of a backup file a wait has got through, as "18.27 MB of 48.32 MB". */
@Composable
internal fun backupReadAmount(progress: BackupReadProgress): String = stringResource(
    MR.strings.backup_read_amount,
    fileSize((progress.fraction * progress.fileSize).toLong()),
    fileSize(progress.fileSize),
)
