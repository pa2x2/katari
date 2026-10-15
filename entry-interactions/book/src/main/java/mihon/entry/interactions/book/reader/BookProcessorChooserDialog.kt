package mihon.entry.interactions.book.reader

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import mihon.entry.interactions.book.R
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.Checkbox
import mihon.presentation.kit.control.RadioButton
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
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
                            .padding(vertical = KatariTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedId == choice.id,
                            onClick = null,
                        )
                        Spacer(Modifier.width(KatariTheme.spacing.m))
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
                        .padding(top = KatariTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = null,
                    )
                    Spacer(Modifier.width(KatariTheme.spacing.m))
                    Text(stringResource(R.string.book_reader_remember_choice))
                }
            }
        }
    }
}
