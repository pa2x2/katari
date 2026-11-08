package app.katari.sync.ui.link

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.sync.client.account.LinkCodeLink
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Asks before joining with a linked device's code (D027), naming the account and its server, and the device that
 * showed the code when the scanned code or the server names it. [problem] is why the last try failed, shown until the
 * next.
 */
@Composable
internal fun JoinConfirmDialog(
    link: LinkCodeLink,
    joining: Boolean,
    problem: String?,
    onJoin: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val server = link.server.host
    Dialog(
        onDismissRequest = onDismissRequest,
        title = link.accountName
            ?.let { stringResource(MR.strings.sync_join_title, it, server) }
            ?: stringResource(MR.strings.sync_join_title_unnamed, server),
        confirm = DialogAction(
            label = stringResource(if (joining) MR.strings.sync_joining else MR.strings.sync_action_join),
            onClick = onJoin,
            enabled = !joining,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
        content = if (link.fromDevice == null && problem == null) {
            null
        } else {
            {
                Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
                    link.fromDevice?.let { Text(stringResource(MR.strings.sync_join_from, it)) }
                    problem?.let { Text(it, tone = ContentTone.Error) }
                }
            }
        },
    )
}
