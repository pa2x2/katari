package eu.kanade.presentation.library.grouping

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import tachiyomi.domain.library.model.LibraryGrouping
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun LibraryGroupingDialog(
    initialGrouping: LibraryGrouping,
    onDismissRequest: () -> Unit,
    onApply: (LibraryGrouping) -> Unit,
) {
    var draftGrouping by remember(initialGrouping) { mutableStateOf(initialGrouping) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.library_grouping_hierarchy)) },
        text = {
            LibraryGroupingEditor(
                grouping = draftGrouping,
                onGroupingChange = { draftGrouping = it },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(draftGrouping) },
            ) {
                Text(stringResource(MR.strings.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}
