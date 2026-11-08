package app.katari.entry.interactions.book.reader

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
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.list.CheckRow
import app.katari.presentation.kit.list.ChoiceRow
import app.katari.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

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
            title = stringResource(MR.strings.book_reader_choose_title),
            confirm = DialogAction(
                label = stringResource(MR.strings.book_reader_confirm),
                onClick = { onChoose(state, selectedId, rememberChoice) },
            ),
            dismiss = DialogAction(label = stringResource(MR.strings.book_reader_close), onClick = onClose),
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
                    title = stringResource(MR.strings.book_reader_remember_choice),
                    checked = rememberChoice,
                    onCheckedChange = { rememberChoice = it },
                    modifier = Modifier.padding(top = KatariTheme.spacing.s),
                )
            }
        }
    }
}
