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
import androidx.compose.ui.semantics.clearAndSetSemantics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.ChoiceTile
import mihon.presentation.kit.control.RadioButton
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun LibraryPinnedStyleChooser(
    selectedStyle: LibraryPinnedDisplayStyle,
    onStyleSelected: (LibraryPinnedDisplayStyle) -> Unit,
) {
    Column {
        SectionHeader(stringResource(MR.strings.pref_library_pinned_display_style))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(
                    start = KatariTheme.spacing.rowInset,
                    end = KatariTheme.spacing.rowInset,
                    bottom = KatariTheme.spacing.s,
                ),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            LibraryPinnedStyleChoice(
                style = LibraryPinnedDisplayStyle.TonalGroup,
                selected = selectedStyle == LibraryPinnedDisplayStyle.TonalGroup,
                onClick = { onStyleSelected(LibraryPinnedDisplayStyle.TonalGroup) },
                modifier = Modifier.weight(1f),
            )
            LibraryPinnedStyleChoice(
                style = LibraryPinnedDisplayStyle.Shelf,
                selected = selectedStyle == LibraryPinnedDisplayStyle.Shelf,
                onClick = { onStyleSelected(LibraryPinnedDisplayStyle.Shelf) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LibraryPinnedStyleChoice(
    style: LibraryPinnedDisplayStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(
        when (style) {
            LibraryPinnedDisplayStyle.TonalGroup -> MR.strings.pref_library_pinned_style_tonal
            LibraryPinnedDisplayStyle.Shelf -> MR.strings.pref_library_pinned_style_shelf
        },
    )
    ChoiceTile(selected = selected, onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier.padding(KatariTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        ) {
            LibraryPinnedStylePreview(style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    role = TextRole.Label,
                )
                RadioButton(
                    selected = selected,
                    onClick = null,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
    }
}
