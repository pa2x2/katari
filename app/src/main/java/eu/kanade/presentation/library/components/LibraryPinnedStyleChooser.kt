package eu.kanade.presentation.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun LibraryPinnedStyleChooser(
    selectedStyle: LibraryPinnedDisplayStyle,
    onStyleSelected: (LibraryPinnedDisplayStyle) -> Unit,
) {
    Column {
        HeadingItem(MR.strings.pref_library_pinned_display_style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SettingsItemsPaddings.Horizontal,
                    end = SettingsItemsPaddings.Horizontal,
                    bottom = SettingsItemsPaddings.Vertical,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LibraryPinnedStylePreview(style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
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

@Composable
private fun LibraryPinnedStylePreview(style: LibraryPinnedDisplayStyle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics {},
    ) {
        when (style) {
            LibraryPinnedDisplayStyle.TonalGroup -> PinnedSectionPreview()
            LibraryPinnedDisplayStyle.Shelf -> PinnedShelfPreview()
        }
    }
}

@Composable
private fun PinnedSectionPreview() {
    PinnedPreviewFrame {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            PreviewSectionCard(Modifier.fillMaxWidth().weight(1f))
            PreviewSectionCard(Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
private fun PinnedShelfPreview() {
    PinnedPreviewFrame {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PreviewShelfCard(Modifier.fillMaxHeight().weight(1f))
            PreviewShelfCard(Modifier.fillMaxHeight().weight(1f))
            PreviewShelfCard(Modifier.fillMaxHeight().width(8.dp))
        }
    }
}

@Composable
private fun PinnedPreviewFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PreviewHeading()
        content()
    }
}

@Composable
private fun PreviewHeading() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
        )
    }
}

@Composable
private fun PreviewSectionCard(modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.45f)),
        )
    }
}

@Composable
private fun PreviewShelfCard(modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.45f)),
        )
    }
}
