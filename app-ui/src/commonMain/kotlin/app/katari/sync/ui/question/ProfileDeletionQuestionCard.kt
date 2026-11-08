package app.katari.sync.ui.question

import androidx.compose.runtime.Composable
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.presentation.kit.list.RowAction
import mihon.presentation.kit.list.RowActions
import mihon.presentation.kit.list.SettingsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A profile deleted on one device while another changed things in it, kept until the person deletes or keeps it. */
@Composable
internal fun ProfileDeletionQuestionCard(
    question: NamedQuestion.ProfileDeletion,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
) {
    SettingsRow(
        title = stringResource(
            MR.strings.sync_question_profile_title,
            question.profileName ?: stringResource(MR.strings.sync_unknown_profile),
        ),
        subtitle = stringResource(
            MR.strings.sync_question_profile_text,
            question.deletedOn.inText(),
            question.changedOn.inText(),
        ),
        icon = MaterialSymbols.Rounded.Delete,
    )
    RowActions(
        listOf(
            RowAction(label = stringResource(MR.strings.action_delete), onClick = onDelete, destructive = true),
            RowAction(label = stringResource(MR.strings.sync_action_keep_profile), onClick = onKeep),
        ),
    )
}
