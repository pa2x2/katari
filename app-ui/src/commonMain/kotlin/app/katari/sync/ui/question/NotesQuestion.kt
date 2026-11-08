package app.katari.sync.ui.question

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import eu.kanade.presentation.util.relativeTimeSpanString
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.EditNote
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.RowAction
import mihon.presentation.kit.list.RowActions
import mihon.presentation.kit.list.SettingsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Notes edited on two devices before they synced (D008), with Choose opening the versions. */
@Composable
internal fun NotesQuestionCard(question: NamedQuestion.Notes, onChoose: () -> Unit) {
    EntryQuestionHeader(
        title = stringResource(MR.strings.sync_question_notes_title, entryTitle(question)),
        profile = question.profile,
        text = stringResource(MR.strings.sync_question_notes_text),
        icon = MaterialSymbols.Rounded.EditNote,
    )
    RowActions(listOf(RowAction(stringResource(MR.strings.action_choose), onChoose)))
}

/**
 * Each version of the notes as its device wrote it, this device's marked (D005); choosing one keeps it everywhere, and
 * Keep both keeps them together, the later first (D008).
 */
@Composable
internal fun NotesVersionsDialog(
    question: NamedQuestion.Notes,
    onKeep: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.sync_question_notes_title, entryTitle(question)),
        confirm = DialogAction(stringResource(MR.strings.sync_action_keep_both), { onKeep(question.bothKept()) }),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column {
            question.versions.forEach { version ->
                SettingsRow(
                    title = version.notes,
                    subtitle = stringResource(
                        MR.strings.sync_question_notes_version,
                        version.made.on.label(),
                        relativeTimeSpanString(version.made.atMillis),
                    ),
                )
                RowActions(
                    listOf(RowAction(stringResource(MR.strings.sync_action_keep_these), { onKeep(version.notes) })),
                )
            }
        }
    }
}

@Composable
private fun entryTitle(question: NamedQuestion.Notes) =
    question.entryTitle ?: stringResource(MR.strings.sync_unknown_entry)

/** Every version in one, the later first, a blank line apart; empty and repeated versions are left out. */
internal fun NamedQuestion.Notes.bothKept(): String = versions
    .sortedByDescending { it.made.atMillis }
    .map { it.notes.trim() }
    .filter(String::isNotEmpty)
    .distinct()
    .joinToString("\n\n")
