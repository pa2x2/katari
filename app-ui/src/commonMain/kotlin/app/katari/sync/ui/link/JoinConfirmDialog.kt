package app.katari.sync.ui.link

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.sync.client.account.LinkCodeLink
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Asks before joining with a linked device's code (D027): which server, and which account where the code says so. A
 * typed code says only the server; [problem] is why the last try failed, shown until the next.
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
            ?.let { stringResource(MR.strings.sync_join_title, it) }
            ?: stringResource(MR.strings.sync_join_title_unnamed),
        confirm = DialogAction(
            label = stringResource(if (joining) MR.strings.sync_joining else MR.strings.sync_action_join),
            onClick = onJoin,
            enabled = !joining,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
            Text(
                link.accountName
                    ?.let { stringResource(MR.strings.sync_join_text, server, it) }
                    ?: stringResource(MR.strings.sync_join_text_unnamed, server),
            )
            link.fromDevice?.let { Text(stringResource(MR.strings.sync_join_from, it)) }
            problem?.let { Text(it, tone = ContentTone.Error) }
        }
    }
}
