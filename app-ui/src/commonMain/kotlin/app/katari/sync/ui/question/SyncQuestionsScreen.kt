package app.katari.sync.ui.question

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.feedback.SnackbarDuration
import app.katari.presentation.kit.feedback.SnackbarHostState
import app.katari.presentation.kit.feedback.SnackbarResult
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupNote
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.Scaffold
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Needs your answer (D029): one card per question, saying what happened, on which devices, and what every device
 * shows until it is answered, with the answers that settle it. Opened from the Sync rows and an entry's banner, it goes
 * back there once the last question is answered.
 */
data object SyncQuestionsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = metroViewModel<SyncQuestionsViewModel>()
        val open by model.open.collectAsState()
        // The entry key of the notes question whose versions are shown
        var notesShown by rememberSaveable { mutableStateOf<String?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val context = currentPlatformContext()

        LaunchedEffect(model) { model.allAnswered.collect { navigator.pop() } }

        // The deletion is written once the message goes without Undo, or once the screen is left while it shows
        val deleteProfile: (NamedQuestion.ProfileDeletion) -> Unit = { question ->
            model.startDeletion(question)
            scope.launch {
                var undone = false
                try {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    undone = snackbarHostState.showSnackbar(
                        message = context.stringResource(
                            MR.strings.sync_question_profile_deleted,
                            question.profileName ?: context.stringResource(MR.strings.sync_unknown_profile),
                        ),
                        actionLabel = context.stringResource(MR.strings.action_undo),
                        duration = SnackbarDuration.Short,
                    ) == SnackbarResult.ActionPerformed
                } finally {
                    if (undone) model.undoDeletion(question) else model.deleteProfile(question)
                }
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.sync_questions_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
            snackbarHostState = snackbarHostState,
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
                                when (question) {
                                    is NamedQuestion.Migration -> MigrationQuestionCard(
                                        question = question,
                                        onKeep = { model.keepMigration(question, it) },
                                        onOpenEntry = { navigator.push(EntryScreen(it)) },
                                    )
                                    is NamedQuestion.Notes -> NotesQuestionCard(
                                        question = question,
                                        onChoose = { notesShown = question.question.entryKey },
                                    )
                                    is NamedQuestion.ProfileDeletion -> ProfileDeletionQuestionCard(
                                        question = question,
                                        onDelete = { deleteProfile(question) },
                                        onKeep = { model.keepProfile(question) },
                                    )
                                }
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
