package eu.kanade.presentation.library.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.control.ChoiceTile
import app.katari.presentation.kit.control.RadioButton
import app.katari.presentation.kit.list.ChoiceTileGrid
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun LibraryPinnedStyleChooser(
    selectedStyle: LibraryPinnedDisplayStyle,
    onStyleSelected: (LibraryPinnedDisplayStyle) -> Unit,
    setting: Preference<*>,
) {
    Column {
        RowGroupHeader(stringResource(MR.strings.pref_library_pinned_display_style), setting = setting)
        ChoiceTileGrid(LibraryPinnedDisplayStyle.entries) { style ->
            LibraryPinnedStyleChoice(
                style = style,
                selected = selectedStyle == style,
                onClick = { onStyleSelected(style) },
            )
        }
    }
}

@Composable
private fun LibraryPinnedStyleChoice(
    style: LibraryPinnedDisplayStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(style.labelRes)
    ChoiceTile(selected = selected, onClick = onClick) {
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
