package eu.kanade.presentation.library.settings

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import mihon.presentation.kit.content.SchematicPart
import mihon.presentation.kit.content.schematic
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.library.model.LibraryDisplayMode

/** A miniature of the library laid out in [mode]: covers as tinted blocks, titles as lines. */
@Composable
internal fun LibraryDisplayModePreview(mode: LibraryDisplayMode) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PREVIEW_ASPECT_RATIO)
            .schematic(SchematicPart.Backdrop)
            .padding(KatariTheme.spacing.xs)
            .clearAndSetSemantics {},
    ) {
        when (mode) {
            LibraryDisplayMode.CompactGrid -> PreviewGrid { PreviewCover(Modifier.fillMaxSize(), titleInside = true) }
            LibraryDisplayMode.ComfortableGrid -> PreviewGrid {
                Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs)) {
                    PreviewCover(Modifier.fillMaxWidth().weight(1f))
                    PreviewLine(Modifier.fillMaxWidth())
                }
            }
            LibraryDisplayMode.CoverOnlyGrid -> PreviewGrid { PreviewCover(Modifier.fillMaxSize()) }
            LibraryDisplayMode.ComfortableList -> PreviewRows(count = 2) {
                PreviewCover(Modifier.fillMaxHeight().aspectRatio(LIST_COVER_ASPECT_RATIO))
                Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs)) {
                    PreviewLine(Modifier.fillMaxWidth())
                    PreviewLine(Modifier.fillMaxWidth(SHORT_LINE_FRACTION))
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
    ) {
        repeat(2) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
    ) {
        repeat(count) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
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
            .schematic(SchematicPart.Block),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (titleInside) {
            PreviewLine(Modifier.fillMaxWidth().padding(KatariTheme.spacing.xxs))
        }
    }
}

@Composable
private fun PreviewLine(modifier: Modifier) {
    Box(
        modifier = modifier
            .height(KatariTheme.spacing.xxs)
            .schematic(SchematicPart.Line),
    )
}

private const val PREVIEW_ASPECT_RATIO = 0.8f
private const val LIST_COVER_ASPECT_RATIO = 0.7f
private const val SHORT_LINE_FRACTION = 0.6f
