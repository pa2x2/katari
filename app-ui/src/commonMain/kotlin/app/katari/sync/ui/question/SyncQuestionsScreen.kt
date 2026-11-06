package app.katari.sync.ui.question

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.katari.sync.client.kind.question.ProfileDeletionAnswer
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.relativeTimeSpanString
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.EditNote
import mihon.icons.materialsymbols.rounded.SwapHoriz
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowAction
import mihon.presentation.kit.list.RowActions
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Needs your answer (D029): one card per question, saying what happened, on which devices, and what every device
 * shows until it is answered, with the answers that settle it. Opened from the Sync rows and an entry's banner.
 */
data object SyncQuestionsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = metroViewModel<SyncQuestionsViewModel>()
        val open by model.open.collectAsState()
        // The entry key of the notes question whose versions are shown
        var notesShown by rememberSaveable { mutableStateOf<String?>(null) }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.sync_questions_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            RowList(contentPadding = contentPadding) {
                val questions = open ?: return@RowList
                if (questions.isEmpty()) {
                    item { RowGroupNote(stringResource(MR.strings.sync_questions_none)) }
                }
                questions.forEachIndexed { index, question ->
                    if (index > 0) item { RowGroupGap() }
                    item {
                        RowGroupItem(RowGroupPosition.Alone) {
                            Column {
                                QuestionCard(
                                    question = question,
                                    onKeepMigration = model::keepMigration,
                                    onShowNotes = { notesShown = it.question.entryKey },
                                    onAnswerProfile = model::answerProfileDeletion,
                                )
                            }
                        }
                    }
                }
            }
        }

        val notes = open?.filterIsInstance<NamedQuestion.Notes>()?.firstOrNull { it.question.entryKey == notesShown }
        if (notes != null) {
            NotesVersionsDialog(
                question = notes,
                onKeep = {
                    model.keepNotes(notes, it)
                    notesShown = null
                },
                onDismissRequest = { notesShown = null },
            )
        }
    }
}

@Composable
private fun QuestionCard(
    question: NamedQuestion,
    onKeepMigration: (NamedQuestion.Migration, String) -> Unit,
    onShowNotes: (NamedQuestion.Notes) -> Unit,
    onAnswerProfile: (NamedQuestion.ProfileDeletion, ProfileDeletionAnswer) -> Unit,
) {
    val unknownDevice = stringResource(MR.strings.sync_unknown_device)
    when (question) {
        is NamedQuestion.Migration -> {
            val title = question.entryTitle ?: stringResource(MR.strings.sync_unknown_entry)
            val moves = question.targets.map {
                stringResource(
                    MR.strings.sync_question_migration_move,
                    it.device ?: unknownDevice,
                    it.source ?: title,
                )
            }
            val now = question.provisional?.let {
                stringResource(MR.strings.sync_question_migration_now, it.source ?: title)
            }
            SettingsRow(
                title = stringResource(MR.strings.sync_question_migration_title, title),
                subtitle = (moves.joinToString(", ") + ".").let { if (now != null) "$it $now" else it },
                icon = MaterialSymbols.Rounded.SwapHoriz,
            )
            // The one followed for now is the proposed answer, last
            RowActions(
                question.targets.sortedBy { it.key == question.question.provisional }.map { target ->
                    RowAction(
                        label = stringResource(MR.strings.sync_action_keep_source, target.source ?: title),
                        onClick = { onKeepMigration(question, target.key) },
                    )
                },
            )
        }
        is NamedQuestion.Notes -> {
            SettingsRow(
                title = stringResource(
                    MR.strings.sync_question_notes_title,
                    question.entryTitle ?: stringResource(MR.strings.sync_unknown_entry),
                ),
                subtitle = stringResource(MR.strings.sync_question_notes_text),
                icon = MaterialSymbols.Rounded.EditNote,
            )
            RowActions(listOf(RowAction(stringResource(MR.strings.action_choose), { onShowNotes(question) })))
        }
        is NamedQuestion.ProfileDeletion -> {
            val name = question.profileName ?: stringResource(MR.strings.sync_unknown_profile)
            SettingsRow(
                title = stringResource(MR.strings.sync_question_profile_title, name),
                subtitle = stringResource(
                    MR.strings.sync_question_profile_text,
                    question.deletedOn ?: unknownDevice,
                    question.changedOn ?: unknownDevice,
                ),
                icon = MaterialSymbols.Rounded.Delete,
            )
            RowActions(
                listOf(
                    RowAction(
                        label = stringResource(MR.strings.action_delete),
                        onClick = { onAnswerProfile(question, ProfileDeletionAnswer.DELETE) },
                        destructive = true,
                    ),
                    RowAction(
                        label = stringResource(MR.strings.sync_action_keep_profile),
                        onClick = { onAnswerProfile(question, ProfileDeletionAnswer.KEEP) },
                    ),
                ),
            )
        }
    }
}

/** Each version of the notes as its device wrote it; choosing one keeps it everywhere. */
@Composable
private fun NotesVersionsDialog(question: NamedQuestion.Notes, onKeep: (String) -> Unit, onDismissRequest: () -> Unit) {
    val unknownDevice = stringResource(MR.strings.sync_unknown_device)
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(
            MR.strings.sync_question_notes_title,
            question.entryTitle ?: stringResource(MR.strings.sync_unknown_entry),
        ),
        confirm = null,
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column {
            question.versions.forEach { version ->
                SettingsRow(
                    title = version.notes,
                    subtitle = stringResource(
                        MR.strings.sync_question_notes_version,
                        version.device ?: unknownDevice,
                        relativeTimeSpanString(version.atMillis),
                    ),
                )
                RowActions(
                    listOf(RowAction(stringResource(MR.strings.sync_action_keep_these), { onKeep(version.notes) })),
                )
            }
        }
    }
}
