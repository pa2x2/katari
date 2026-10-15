package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import dev.icerock.moko.resources.StringResource
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.LibraryMetrics
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme

/**
 * The action at the cover's trailing corner: while selecting it shows whether the item is selected, replacing the
 * resume button so a tap meant to select can't open the entry.
 */
internal fun coverTrailingAction(
    selectionMode: Boolean,
    isSelected: Boolean,
    onClickContinueReading: (() -> Unit)?,
    continueReadingProgress: Float?,
    continueReadingContentDescription: StringResource,
    large: Boolean,
    onCover: Boolean = true,
): (@Composable (Modifier) -> Unit)? {
    if (selectionMode) {
        return { modifier -> SelectionIndicator(isSelected = isSelected, onCover = onCover, modifier = modifier) }
    }
    if (onClickContinueReading == null) return null
    return { modifier ->
        ContinueReadingButton(
            size = if (large) LibraryMetrics.resumeButtonLarge else LibraryMetrics.resumeButtonSmall,
            iconSize = if (large) LibraryMetrics.resumeIconLarge else LibraryMetrics.resumeIconSmall,
            onClick = onClickContinueReading,
            progress = continueReadingProgress,
            contentDescription = continueReadingContentDescription,
            modifier = modifier,
        )
    }
}

@Composable
private fun SelectionIndicator(
    isSelected: Boolean,
    onCover: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = KatariTheme.colors
    val shape = KatariTheme.shapes.full
    if (isSelected) {
        Surface(
            modifier = modifier.size(KatariTheme.sizes.icon),
            container = SurfaceContainer.Primary,
            shape = shape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .size(KatariTheme.sizes.icon)
                .clip(shape)
                .background(if (onCover) colors.mediaScrim else Color.Transparent)
                .border(KatariTheme.sizes.selectedBorder, if (onCover) colors.onMedia else colors.outline, shape),
        )
    }
}
