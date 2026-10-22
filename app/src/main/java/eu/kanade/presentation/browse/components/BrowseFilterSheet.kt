package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.RestartAlt
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.KatariTheme
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
                    { close ->
                        DropdownMenuItem(
                            label = stringResource(MR.strings.action_reset),
                            onClick = {
                                reset()
                                close()
                            },
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.l),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(label = stringResource(MR.strings.action_done), onClick = onDismissRequest)
            }
        }
    }
}
