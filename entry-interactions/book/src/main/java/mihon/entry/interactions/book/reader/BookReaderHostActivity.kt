package mihon.entry.interactions.book

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mihon.book.api.BookFailure
import mihon.entry.interactions.EntryInteractionActivity
import mihon.entry.interactions.setEntryInteractionContent
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Generic BOOK host for processor selection, launch, and structured unsupported-content failures. */
internal class BookReaderHostActivity : EntryInteractionActivity() {
    private var hostState by mutableStateOf<BookReaderHostState?>(null)
    private var launchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.book_reader_host_title)
        setEntryInteractionContent {
            BookReaderHostContent(
                state = hostState,
                onChoose = ::chooseProcessor,
                onClose = ::finish,
            )
        }

        lifecycleScope.launch {
            val entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L)
            val chapterId = intent.getLongExtra(EXTRA_CHAPTER_ID, -1L)
            val state = if (entryId < 0L || chapterId < 0L) {
                BookReaderHostState.Unavailable(
                    BookFailure(
                        reason = mihon.book.api.BookFailureReason.CONTENT_UNAVAILABLE,
                        message = getString(R.string.book_reader_invalid_request),
                    ),
                )
            } else {
                Injekt.get<BookReaderHostResolver>().resolve(entryId, chapterId)
            }
            handle(state)
        }
    }

    private fun chooseProcessor(
        state: BookReaderHostState.ChoiceRequired,
        processorId: String,
        remember: Boolean,
    ) {
        handle(
            Injekt.get<BookReaderHostResolver>().choose(
                state = state,
                processorId = processorId,
                remember = remember,
            ),
        )
    }

    private fun handle(state: BookReaderHostState) {
        when (state) {
            is BookReaderHostState.ReaderSelected -> launch(state)
            else -> hostState = state
        }
    }

    private fun launch(state: BookReaderHostState.ReaderSelected) {
        launchJob?.cancel()
        hostState = null
        launchJob = lifecycleScope.launch {
            val result = Injekt.get<BookReaderSessionFactory>().openPrepared(
                context = this@BookReaderHostActivity,
                prepared = state.prepared,
                processorId = state.processor.id,
            )
            when (result) {
                is BookReaderOpenResult.Failure -> {
                    hostState = BookReaderHostState.Unavailable(
                        failure = result.failure,
                        descriptor = state.descriptor,
                    )
                }
                is BookReaderOpenResult.Success -> {
                    val registry = Injekt.get<BookReaderSessionRegistry>()
                    val token = registry.register(result.session)
                    try {
                        startActivity(
                            state.processor.createReaderIntent(
                                context = this@BookReaderHostActivity,
                                request = state.prepared.request,
                                sessionToken = token,
                            ),
                        )
                        finish()
                    } catch (error: Exception) {
                        registry.discard(token)
                        hostState = BookReaderHostState.Unavailable(
                            failure = BookFailure(
                                reason = mihon.book.api.BookFailureReason.PROCESSOR_UNAVAILABLE,
                                message = error.message ?: "The selected book reader could not be started.",
                            ),
                            descriptor = state.descriptor,
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_ENTRY_ID = "entry_id"
        private const val EXTRA_CHAPTER_ID = "chapter_id"

        fun newIntent(context: Context, entry: Entry, chapter: EntryChapter): Intent {
            entry.requireBook()
            return Intent(context, BookReaderHostActivity::class.java).apply {
                putExtra(EXTRA_ENTRY_ID, entry.id)
                putExtra(EXTRA_CHAPTER_ID, chapter.id)
            }
        }
    }
}

@Composable
private fun BookReaderHostContent(
    state: BookReaderHostState?,
    onChoose: (BookReaderHostState.ChoiceRequired, String, Boolean) -> Unit,
    onClose: () -> Unit,
) {
    when (state) {
        null -> BookReaderLoadingScreen(
            contentDescription = stringResource(R.string.book_reader_loading),
        )
        is BookReaderHostState.Unavailable -> BookReaderErrorScreen(
            title = stringResource(R.string.book_reader_unavailable_title),
            message = stringResource(
                R.string.book_reader_unavailable_message,
                state.failure.reason.displayName(),
                state.failure.message,
            ),
            closeLabel = stringResource(R.string.book_reader_close),
            onClose = onClose,
        )
        is BookReaderHostState.ChoiceRequired -> BookProcessorChooserDialog(
            state = state,
            onChoose = onChoose,
            onClose = onClose,
        )
        is BookReaderHostState.ReaderSelected -> Unit
    }
}

@Composable
private fun BookProcessorChooserDialog(
    state: BookReaderHostState.ChoiceRequired,
    onChoose: (BookReaderHostState.ChoiceRequired, String, Boolean) -> Unit,
    onClose: () -> Unit,
) {
    var selectedId by remember(state) { mutableStateOf(state.choices.first().id) }
    var rememberChoice by remember(state) { mutableStateOf(false) }

    Box {
        BookReaderDialogBackground()
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text(stringResource(R.string.book_reader_choose_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.choices.forEach { choice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedId == choice.id,
                                    role = Role.RadioButton,
                                    onClick = { selectedId = choice.id },
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedId == choice.id,
                                onClick = null,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(choice.displayName)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = rememberChoice,
                                role = Role.Checkbox,
                                onValueChange = { rememberChoice = it },
                            )
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = rememberChoice,
                            onCheckedChange = null,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.book_reader_remember_choice))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onChoose(state, selectedId, rememberChoice) }) {
                    Text(stringResource(R.string.book_reader_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.book_reader_close))
                }
            },
        )
    }
}

internal fun mihon.book.api.BookFailureReason.displayName(): String {
    return name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
}
