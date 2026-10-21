package eu.kanade.presentation.library.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.ChoiceTile
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Display modes as small drawings of the layout they produce, so the names don't have to be decoded. */
@Composable
internal fun LibraryDisplayModeChooser(
    selectedMode: LibraryDisplayMode,
    onModeSelected: (LibraryDisplayMode) -> Unit,
) {
    Column {
        RowGroupHeader(stringResource(MR.strings.action_display_mode))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(horizontal = KatariTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        ) {
            DisplayModes.forEach { mode ->
                LibraryDisplayModeChoice(
                    mode = mode,
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val DisplayModes = listOf(
    LibraryDisplayMode.CompactGrid,
    LibraryDisplayMode.ComfortableGrid,
    LibraryDisplayMode.ComfortableList,
    LibraryDisplayMode.CoverOnlyGrid,
    LibraryDisplayMode.List,
)

@Composable
private fun LibraryDisplayModeChoice(
    mode: LibraryDisplayMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(
        when (mode) {
            LibraryDisplayMode.CompactGrid -> MR.strings.action_display_grid
            LibraryDisplayMode.ComfortableGrid -> MR.strings.action_display_comfortable_grid
            LibraryDisplayMode.ComfortableList -> MR.strings.action_display_comfortable_list
            LibraryDisplayMode.CoverOnlyGrid -> MR.strings.action_display_cover_only_grid
            LibraryDisplayMode.List -> MR.strings.action_display_list
        },
    )
    ChoiceTile(selected = selected, onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier.padding(KatariTheme.spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        ) {
            LibraryDisplayModePreview(mode)
            Text(
                text = label,
                role = TextRole.Badge,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
            )
        }
    }
}
