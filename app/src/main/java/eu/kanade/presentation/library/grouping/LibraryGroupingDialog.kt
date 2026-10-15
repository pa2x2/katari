package eu.kanade.presentation.library.grouping

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import tachiyomi.domain.library.model.LibraryGrouping
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun LibraryGroupingDialog(
    initialGrouping: LibraryGrouping,
    onDismissRequest: () -> Unit,
    onApply: (LibraryGrouping) -> Unit,
) {
    var draftGrouping by remember(initialGrouping) { mutableStateOf(initialGrouping) }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.library_grouping_hierarchy),
        confirm = DialogAction(stringResource(MR.strings.action_apply), { onApply(draftGrouping) }),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        LibraryGroupingEditor(
            grouping = draftGrouping,
            onGroupingChange = { draftGrouping = it },
        )
    }
}
