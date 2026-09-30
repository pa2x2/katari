package eu.kanade.presentation.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.i18n.stringResource

/** Display modes as small drawings of the layout they produce, so the names don't have to be decoded. */
@Composable
internal fun LibraryDisplayModeChooser(
    selectedMode: LibraryDisplayMode,
    onModeSelected: (LibraryDisplayMode) -> Unit,
) {
    Column {
        HeadingItem(MR.strings.action_display_mode)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(
                    start = SettingsItemsPaddings.Horizontal,
                    end = SettingsItemsPaddings.Horizontal,
                    bottom = SettingsItemsPaddings.Vertical,
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
    Surface(
        modifier = modifier.selectable(
            selected = selected,
            onClick = onClick,
            role = Role.RadioButton,
        ),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LibraryDisplayModePreview(mode)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun LibraryDisplayModePreview(mode: LibraryDisplayMode) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
            .clearAndSetSemantics {},
    ) {
        when (mode) {
            LibraryDisplayMode.CompactGrid -> PreviewGrid { PreviewCover(Modifier.fillMaxSize(), titleInside = true) }
            LibraryDisplayMode.ComfortableGrid -> PreviewGrid {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    PreviewCover(Modifier.fillMaxWidth().weight(1f))
                    PreviewLine(Modifier.fillMaxWidth())
                }
            }
            LibraryDisplayMode.CoverOnlyGrid -> PreviewGrid { PreviewCover(Modifier.fillMaxSize()) }
            LibraryDisplayMode.ComfortableList -> PreviewRows(count = 2) {
                PreviewCover(Modifier.fillMaxHeight().aspectRatio(0.7f))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    PreviewLine(Modifier.fillMaxWidth())
                    PreviewLine(Modifier.fillMaxWidth(0.6f))
                }
            }
            LibraryDisplayMode.List -> PreviewRows(count = 4) {
                PreviewCover(Modifier.fillMaxHeight().aspectRatio(1f))
                PreviewLine(Modifier.fillMaxWidth())
            }
        }
    }
}

/** Two rows of three cells, like a portrait grid. */
@Composable
private fun PreviewGrid(cell: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(2) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                repeat(3) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) { cell() }
                }
            }
        }
    }
}

@Composable
private fun PreviewRows(count: Int, row: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(count) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                row()
            }
        }
    }
}

@Composable
private fun PreviewCover(modifier: Modifier, titleInside: Boolean = false) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (titleInside) {
            PreviewLine(Modifier.fillMaxWidth().padding(2.dp))
        }
    }
}

@Composable
private fun PreviewLine(modifier: Modifier) {
    Box(
        modifier = modifier
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)),
    )
}
