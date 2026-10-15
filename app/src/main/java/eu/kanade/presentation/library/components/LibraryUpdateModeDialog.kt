package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.ChoiceRow
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** Sets how library updates treat every selected entry; picking a mode applies it. */
@Composable
fun LibraryUpdateModeDialog(
    entryCount: Int,
    currentMode: EntryUpdateMode?,
    onDismissRequest: () -> Unit,
    onModeSelected: (EntryUpdateMode) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = pluralStringResource(MR.plurals.library_update_mode_title, entryCount, entryCount),
        confirm = null,
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column {
            EntryUpdateMode.entries.forEach { mode ->
                ChoiceRow(
                    title = stringResource(
                        when (mode) {
                            EntryUpdateMode.FOLLOW_RULES -> MR.strings.entry_updates_mode_follow
                            EntryUpdateMode.ALWAYS -> MR.strings.entry_updates_mode_always
                            EntryUpdateMode.NEVER -> MR.strings.entry_updates_mode_never
                        },
                    ),
                    selected = mode == currentMode,
                    onClick = {
                        onModeSelected(mode)
                        onDismissRequest()
                    },
                )
            }
        }
    }
}
