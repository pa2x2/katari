package eu.kanade.tachiyomi.ui.browse.feed.switcher

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredroundedfilled.ViewList
import mihon.icons.materialsymbols.roundedfilled.ViewCompact
import mihon.icons.materialsymbols.roundedfilled.ViewModule
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.RadioMenuItem
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The display modes a feed offers: the same ones, in the same order, as the source's catalog. */
private val FeedDisplayModes = listOf(
    LibraryDisplayMode.ComfortableGrid to MR.strings.action_display_comfortable_grid,
    LibraryDisplayMode.ComfortableList to MR.strings.action_display_comfortable_list,
    LibraryDisplayMode.CompactGrid to MR.strings.action_display_grid,
    LibraryDisplayMode.List to MR.strings.action_display_list,
)

@Composable
internal fun FeedDisplayModeButton(
    displayMode: LibraryDisplayMode,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            icon = when (displayMode) {
                LibraryDisplayMode.List,
                LibraryDisplayMode.ComfortableList,
                -> MaterialSymbols.AutoMirroredRoundedFilled.ViewList
                LibraryDisplayMode.ComfortableGrid -> MaterialSymbols.RoundedFilled.ViewModule
                LibraryDisplayMode.CompactGrid,
                LibraryDisplayMode.CoverOnlyGrid,
                -> MaterialSymbols.RoundedFilled.ViewCompact
            },
            contentDescription = stringResource(MR.strings.action_display_mode),
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            FeedDisplayModes.forEach { (mode, title) ->
                RadioMenuItem(
                    label = stringResource(title),
                    selected = displayMode == mode,
                    onClick = {
                        expanded = false
                        onDisplayModeChange(mode)
                    },
                )
            }
        }
    }
}
