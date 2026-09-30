package eu.kanade.tachiyomi.ui.browse.feed.switcher

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.RadioMenuItem
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
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = if (displayMode == LibraryDisplayMode.List ||
                    displayMode == LibraryDisplayMode.ComfortableList
                ) {
                    Icons.AutoMirrored.Filled.ViewList
                } else {
                    Icons.Filled.ViewModule
                },
                contentDescription = stringResource(MR.strings.action_display_mode),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            FeedDisplayModes.forEach { (mode, title) ->
                RadioMenuItem(
                    text = { Text(text = stringResource(title)) },
                    isChecked = displayMode == mode,
                ) {
                    expanded = false
                    onDisplayModeChange(mode)
                }
            }
        }
    }
}
