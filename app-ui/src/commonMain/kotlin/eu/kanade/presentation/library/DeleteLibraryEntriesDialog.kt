package eu.kanade.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.icerock.moko.resources.StringResource
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.CheckRow
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun DeleteLibraryEntriesDialog(
    containsLocalEntries: Boolean,
    containsMergedEntries: Boolean = false,
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean, Boolean) -> Unit,
) {
    var list by remember {
        mutableStateOf(
            buildList<CheckboxState.State<StringResource>> {
                add(CheckboxState.State.None(MR.strings.manga_from_library))
                if (!containsLocalEntries) {
                    add(CheckboxState.State.None(MR.strings.downloaded_chapters))
                }
            },
        )
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.action_remove),
        icon = MaterialSymbols.Rounded.Delete,
        confirm = DialogAction(
            label = stringResource(MR.strings.action_remove),
            onClick = {
                onDismissRequest()
                onConfirm(
                    list[0].isChecked,
                    list.getOrElse(1) { CheckboxState.State.None(0) }.isChecked,
                )
            },
            enabled = list.any { it.isChecked },
            destructive = true,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            if (containsMergedEntries) {
                Text(text = stringResource(MR.strings.remove_merged_manga_warning))
            }
            list.forEach { state ->
                CheckRow(
                    title = stringResource(state.value),
                    checked = state.isChecked,
                    onCheckedChange = {
                        val index = list.indexOf(state)
                        if (index != -1) {
                            val mutableList = list.toMutableList()
                            mutableList[index] = state.next() as CheckboxState.State<StringResource>
                            list = mutableList.toList()
                        }
                    },
                )
            }
        }
    }
}
