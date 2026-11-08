package app.katari.sync.ui.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.presentation.kit.feedback.rememberScreenMessages
import app.katari.sync.ui.handoff.EntryHandOffViewModel
import app.katari.sync.ui.handoff.HandOffNote
import app.katari.sync.ui.handoff.LeftOfflineViewModel
import app.katari.sync.ui.question.EntryQuestion
import app.katari.sync.ui.question.EntryQuestionViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** What sync shows on an entry's screen: the note beside Resume, and the question waiting about the entry. */
class EntrySync(
    val handOffNote: HandOffNote?,
    val question: EntryQuestion?,
    val onChooseQuestion: () -> Unit,
) {
    companion object {
        val NONE = EntrySync(handOffNote = null, question = null, onChooseQuestion = {})
    }
}

/**
 * Opening the entry [entryId] fetches its progress from the other devices first (D052), and coming back to it from a
 * reader while sync can't send says the reading stays here for now (D054).
 */
@Composable
fun rememberEntrySync(entryId: Long, onChooseQuestion: () -> Unit): EntrySync {
    val handOff = assistedMetroViewModel<EntryHandOffViewModel, EntryHandOffViewModel.Factory>(
        key = "sync-hand-off-$entryId",
    ) { create(entryId) }
    val questions = assistedMetroViewModel<EntryQuestionViewModel, EntryQuestionViewModel.Factory>(
        key = "sync-question-$entryId",
    ) { create(entryId) }
    val note by handOff.note.collectAsStateWithLifecycle()
    val messages = rememberScreenMessages()
    val savedOffline = stringResource(MR.strings.sync_saved_offline)
    val leftOffline = assistedMetroViewModel<LeftOfflineViewModel, LeftOfflineViewModel.Factory>(
        key = "sync-left-offline-$entryId",
    ) { create(entryId) }
    LaunchedEffect(leftOffline) { leftOffline.savedOffline.collect { messages.show(savedOffline) } }
    LifecycleStartEffect(leftOffline) {
        leftOffline.onShown()
        onStopOrDispose { leftOffline.onHidden() }
    }
    val question by questions.question.collectAsStateWithLifecycle()
    return EntrySync(note, question, onChooseQuestion)
}
