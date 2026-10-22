package mihon.entry.interactions.book.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mihon.entry.interactions.book.R
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.CheckRow
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.theme.KatariTheme

/** Asks which installed reader opens the book when several can, optionally remembering the answer. */
@Composable
internal fun BookProcessorChooserDialog(
    state: BookReaderHostState.ChoiceRequired,
    onChoose: (BookReaderHostState.ChoiceRequired, String, Boolean) -> Unit,
    onClose: () -> Unit,
) {
    var selectedId by remember(state) { mutableStateOf(state.choices.first().id) }
    var rememberChoice by remember(state) { mutableStateOf(false) }

    Box {
        BookReaderDialogBackground()
        Dialog(
            onDismissRequest = onClose,
            title = stringResource(R.string.book_reader_choose_title),
            confirm = DialogAction(
                label = stringResource(R.string.book_reader_confirm),
                onClick = { onChoose(state, selectedId, rememberChoice) },
            ),
            dismiss = DialogAction(label = stringResource(R.string.book_reader_close), onClick = onClose),
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                state.choices.forEach { choice ->
                    ChoiceRow(
                        title = choice.displayName,
                        selected = selectedId == choice.id,
                        onClick = { selectedId = choice.id },
                    )
                }
                CheckRow(
                    title = stringResource(R.string.book_reader_remember_choice),
                    checked = rememberChoice,
                    onCheckedChange = { rememberChoice = it },
                    modifier = Modifier.padding(top = KatariTheme.spacing.s),
                )
            }
        }
    }
}
