package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.entry.components.EntryActionIcons
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.surface.PaneDivider
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Opening the full entry above the library and merge actions, under an entry's preview. */
@Composable
internal fun BrowseEntryPreviewBottomBar(
    favorite: Boolean,
    onOpenEntry: () -> Unit,
    onLibraryAction: () -> Unit,
    onMergeAction: () -> Unit,
) {
    val spacing = KatariTheme.spacing.s

    Surface(container = SurfaceContainer.High) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PaneDivider()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                Button(
                    label = stringResource(MR.strings.action_open),
                    onClick = onOpenEntry,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Tonal,
                    icon = EntryActionIcons.openFullEntry,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    Button(
                        label = stringResource(
                            if (favorite) MR.strings.remove_from_library else MR.strings.add_to_library,
                        ),
                        onClick = onLibraryAction,
                        modifier = Modifier.weight(1f),
                        icon = EntryActionIcons.library(favorite),
                    )
                    Button(
                        label = stringResource(MR.strings.action_add_to_merge),
                        onClick = onMergeAction,
                        modifier = Modifier.weight(1f),
                        style = ButtonStyle.Tonal,
                        icon = EntryActionIcons.merge,
                    )
                }
            }
        }
    }
}
