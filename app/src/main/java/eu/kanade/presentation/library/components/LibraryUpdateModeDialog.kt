package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.RadioItem
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
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(pluralStringResource(MR.plurals.library_update_mode_title, entryCount, entryCount)) },
        text = {
            Column {
                EntryUpdateMode.entries.forEach { mode ->
                    RadioItem(
                        label = stringResource(
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
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}
