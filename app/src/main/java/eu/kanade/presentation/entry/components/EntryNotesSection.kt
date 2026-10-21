package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.EditNote
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.surface.PaneDivider
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun EntryNotesSection(
    content: String,
    expanded: Boolean,
    onEditNotes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (content.isBlank()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EntryNotesDisplay(
            content = content,
            modifier = modifier.fillMaxWidth(),
        )
        if (expanded) {
            Button(
                label = stringResource(MR.strings.action_edit_notes),
                onClick = onEditNotes,
                modifier = Modifier.padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.xs),
                style = ButtonStyle.Text,
                icon = MaterialSymbols.Rounded.EditNote,
            )
        }

        PaneDivider(
            modifier = Modifier
                .padding(
                    top = if (expanded) KatariTheme.sizes.none else KatariTheme.spacing.m,
                    bottom = if (expanded) KatariTheme.spacing.l else KatariTheme.spacing.m,
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun EntryNotesSectionPreview() {
    EntryNotesSection(
        onEditNotes = {},
        expanded = true,
        content = "# Hello world\ntest1234 hi there!",
    )
}
