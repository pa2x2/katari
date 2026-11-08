package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import app.katari.presentation.kit.dialog.ConfirmDialog
import app.katari.presentation.kit.list.ChoiceRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * How far a restore reaches on a device that syncs (D046a): this device, the default, where restored values count as
 * made when the backup was made, or the whole account, which every device returns to.
 */
@Composable
internal fun RestoreReachSection(createdAt: Long, rollBack: Boolean, onRollBackChange: (Boolean) -> Unit) {
    val made = rememberBackupTimeText(createdAt)
    BackupSection(
        title = stringResource(MR.strings.sync_restore_reach),
        rows = listOf(
            {
                ChoiceRow(
                    title = stringResource(MR.strings.sync_restore_here),
                    subtitle = stringResource(MR.strings.sync_restore_here_text, made),
                    selected = !rollBack,
                    onClick = { onRollBackChange(false) },
                )
            },
            {
                ChoiceRow(
                    title = stringResource(MR.strings.sync_restore_rollback),
                    subtitle = stringResource(MR.strings.sync_restore_rollback_text, made),
                    selected = rollBack,
                    onClick = { onRollBackChange(true) },
                )
            },
        ),
    )
}

/** The second ask before an account rollback, saying what every device loses. */
@Composable
internal fun RollBackConfirmDialog(createdAt: Long, onConfirm: () -> Unit, onDismissRequest: () -> Unit) {
    ConfirmDialog(
        title = stringResource(MR.strings.sync_rollback_confirm_title),
        text = stringResource(MR.strings.sync_rollback_confirm_text, rememberBackupTimeText(createdAt)),
        confirmLabel = stringResource(MR.strings.sync_action_roll_back),
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest,
        destructive = true,
    )
}

@Composable
private fun rememberBackupTimeText(createdAt: Long): String {
    if (createdAt <= 0) return stringResource(MR.strings.sync_backup_time_unknown)
    val locale = LocalLocale.current.platformLocale
    return remember(createdAt, locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
            .format(Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()))
    }
}
