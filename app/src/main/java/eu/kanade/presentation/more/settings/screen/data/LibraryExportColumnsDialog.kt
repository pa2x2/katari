package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.data.export.LibraryExporter.ExportOptions
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.CheckRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Picks the columns of the library CSV export. Author and artist are only exported alongside the title. */
@Composable
internal fun LibraryExportColumnsDialog(
    options: ExportOptions,
    onConfirm: (ExportOptions) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var titleSelected by remember { mutableStateOf(options.includeTitle) }
    var authorSelected by remember { mutableStateOf(options.includeAuthor) }
    var artistSelected by remember { mutableStateOf(options.includeArtist) }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.migration_dialog_what_to_include),
        confirm = DialogAction(
            label = stringResource(MR.strings.action_save),
            onClick = {
                onConfirm(
                    ExportOptions(
                        includeTitle = titleSelected,
                        includeAuthor = authorSelected,
                        includeArtist = artistSelected,
                    ),
                )
                onDismissRequest()
            },
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onClick = onDismissRequest),
    ) {
        Column {
            CheckRow(
                title = stringResource(MR.strings.title),
                checked = titleSelected,
                onCheckedChange = { checked ->
                    titleSelected = checked
                    if (!checked) {
                        authorSelected = false
                        artistSelected = false
                    }
                },
            )
            CheckRow(
                title = stringResource(MR.strings.author),
                checked = authorSelected,
                onCheckedChange = { authorSelected = it },
                enabled = titleSelected,
            )
            CheckRow(
                title = stringResource(MR.strings.artist),
                checked = artistSelected,
                onCheckedChange = { artistSelected = it },
                enabled = titleSelected,
            )
        }
    }
}
