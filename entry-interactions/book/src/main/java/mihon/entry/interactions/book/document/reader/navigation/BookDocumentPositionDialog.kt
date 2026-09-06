package mihon.entry.interactions.book.document.reader.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import mihon.entry.interactions.book.R
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.book_navigation_go_to)) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    label = {
                        Text(
                            stringResource(
                                if (snapshot.paged) {
                                    R.string.book_navigation_page
                                } else {
                                    R.string.book_navigation_percentage
                                },
                            ),
                        )
                    },
                    supportingText = { Text(stringResource(R.string.book_navigation_range, min, max)) },
                    isError = input.isNotEmpty() && value == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { go() }),
                )
                Row {
                    TextButton(onClick = { onGo(snapshot.range.start) }) {
                        Text(stringResource(R.string.book_navigation_start))
                    }
                    TextButton(onClick = { onGo(snapshot.range.endInclusive) }) {
                        Text(stringResource(R.string.book_navigation_end))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = value != null, onClick = go) { Text(stringResource(R.string.book_navigation_go)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.book_document_external_link_cancel)) }
        },
    )
}
