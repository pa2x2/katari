package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.RestartAlt
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetActions
import mihon.presentation.kit.sheet.SheetHeader
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A filter sheet for a browse list: a title with an optional reset in its menu, the [content], and a Done button. */
@Composable
fun BrowseFilterSheet(
    title: String,
    onDismissRequest: () -> Unit,
    onReset: (() -> Unit)? = null,
    resetEnabled: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    Sheet(onDismissRequest = onDismissRequest, capHeight = true) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(
                title = title,
                menu = onReset?.let { reset ->
                    {
                        DropdownMenuItem(
                            label = stringResource(MR.strings.action_reset),
                            onClick = reset,
                            enabled = resetEnabled,
                            icon = MaterialSymbols.Rounded.RestartAlt,
                        )
                    }
                },
            )

            content(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            )

            SheetActions(confirm = DialogAction(stringResource(MR.strings.action_done), onDismissRequest))
        }
    }
}
