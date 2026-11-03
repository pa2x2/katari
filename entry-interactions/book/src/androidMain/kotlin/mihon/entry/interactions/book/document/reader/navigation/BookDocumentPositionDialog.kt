package mihon.entry.interactions.book.document.reader.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import mihon.entry.interactions.book.R
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.TextField
import kotlin.math.roundToInt

/** Accessible exact page/percentage entry with bounded input and start/end shortcuts. */
@Composable
internal fun BookDocumentPositionDialog(
    snapshot: BookDocumentSeekSnapshot,
    onGo: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(snapshot.value.roundToInt().toString()) }
    val min = snapshot.range.start.roundToInt()
    val max = snapshot.range.endInclusive.roundToInt()
    val value = input.toIntOrNull()?.takeIf { it in min..max }
    val go: () -> Unit = { value?.let { onGo(it.toFloat()) } }
    Dialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.book_navigation_go_to),
        confirm = DialogAction(
            label = stringResource(R.string.book_navigation_go),
            onClick = go,
            enabled = value != null,
        ),
        dismiss = DialogAction(
            label = stringResource(R.string.book_document_external_link_cancel),
            onClick = onDismiss,
        ),
    ) {
        Column {
            TextField(
                value = input,
                onValueChange = { input = it },
                label = stringResource(
                    if (snapshot.paged) {
                        R.string.book_navigation_page
                    } else {
                        R.string.book_navigation_percentage
                    },
                ),
                supportingText = stringResource(R.string.book_navigation_range, min, max),
                isError = input.isNotEmpty() && value == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { go() }),
            )
            Row {
                Button(
                    label = stringResource(R.string.book_navigation_start),
                    onClick = { onGo(snapshot.range.start) },
                    style = ButtonStyle.Text,
                )
                Button(
                    label = stringResource(R.string.book_navigation_end),
                    onClick = { onGo(snapshot.range.endInclusive) },
                    style = ButtonStyle.Text,
                )
            }
        }
    }
}
