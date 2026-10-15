package eu.kanade.presentation.library.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle

/** A miniature of the pinned section in [style]: a heading over its entries, grouped in rows or on a shelf. */
@Composable
internal fun LibraryPinnedStylePreview(style: LibraryPinnedDisplayStyle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PREVIEW_ASPECT_RATIO)
            .clip(KatariTheme.shapes.small)
            .background(KatariTheme.colors.surfaceVariant)
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
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
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
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        ) {
            PreviewShelfCard(Modifier.fillMaxHeight().weight(1f))
            PreviewShelfCard(Modifier.fillMaxHeight().weight(1f))
            PreviewShelfCard(Modifier.fillMaxHeight().width(KatariTheme.spacing.s))
        }
    }
}

@Composable
private fun PinnedPreviewFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(KatariTheme.spacing.xs)
            .clip(KatariTheme.shapes.small)
            .background(KatariTheme.colors.surfaceContainerHighest)
            .padding(KatariTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        PreviewHeading()
        content()
    }
}

@Composable
private fun PreviewHeading() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
    ) {
        Box(
            modifier = Modifier
                .size(KatariTheme.spacing.xs)
                .clip(KatariTheme.shapes.full)
                .background(KatariTheme.colors.primary),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(HEADING_LINE_FRACTION)
                .height(KatariTheme.spacing.xs)
                .clip(KatariTheme.shapes.full)
                .background(KatariTheme.colors.primary),
        )
    }
}

@Composable
private fun PreviewSectionCard(modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(KatariTheme.shapes.extraSmall)
            .background(KatariTheme.colors.primaryContainer)
            .padding(KatariTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(KatariTheme.spacing.s)
                .clip(KatariTheme.shapes.extraSmall)
                .background(KatariTheme.colors.primary),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(KatariTheme.spacing.xxs)
                .clip(KatariTheme.shapes.full)
                .background(KatariTheme.colors.onPrimaryContainer),
        )
    }
}

@Composable
private fun PreviewShelfCard(modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(KatariTheme.shapes.extraSmall)
            .background(KatariTheme.colors.primaryContainer)
            .padding(KatariTheme.spacing.xxs),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(KatariTheme.shapes.extraSmall)
                .background(KatariTheme.colors.primary),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(SHELF_TITLE_FRACTION)
                .height(KatariTheme.spacing.xxs)
                .clip(KatariTheme.shapes.full)
                .background(KatariTheme.colors.onPrimaryContainer),
        )
    }
}

// The drawing keeps its proportions at any tile width; 3:1 matches its height on a phone.
private const val PREVIEW_ASPECT_RATIO = 3f
private const val HEADING_LINE_FRACTION = 0.2f
private const val SHELF_TITLE_FRACTION = 0.7f
