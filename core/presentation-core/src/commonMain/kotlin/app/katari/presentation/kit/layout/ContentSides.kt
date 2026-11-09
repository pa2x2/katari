package app.katari.presentation.kit.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.navigation.DockClearance
import app.katari.presentation.kit.navigation.LocalDockClearance

/**
 * The room a screen's content keeps on each side, which the kit `Scaffold` works out and keeps out of the padding it
 * hands the screen: the system bars and camera cutout ([insetStart], [insetEnd]), the side dock, and the [frameMargin]s
 * that centre the screen's frame. [start] and [end] hold all of it. The kit's layouts ([RowList], [CardFlow],
 * [CoverGrid], [TwoPanes], [FramedContent] and the rest) place their content inside it and give what they hold none,
 * so it is applied once whatever a screen does with its padding.
 */
@Immutable
internal data class ContentSides(
    val start: Dp = 0.dp,
    val end: Dp = 0.dp,
    val insetStart: Dp = 0.dp,
    val insetEnd: Dp = 0.dp,
    val frameMargin: Dp = 0.dp,
) {
    /** The room as padding on the sides only. */
    val padding: PaddingValues get() = PaddingValues(start = start, end = end)

    /** The system bars and cutout among the room, as padding, for the window insets a layout applying it consumes. */
    val insetPadding: PaddingValues get() = PaddingValues(start = insetStart, end = insetEnd)

    companion object {
        val None = ContentSides()
    }
}

/** The side room of the screen content composed here, until a kit layout applies it. */
internal val LocalContentSides = compositionLocalOf { ContentSides.None }

/** [this] padding with [sides]'s room added on the start and end. */
internal fun PaddingValues.plusSides(sides: ContentSides, direction: LayoutDirection): PaddingValues = PaddingValues(
    start = calculateStartPadding(direction) + sides.start,
    top = calculateTopPadding(),
    end = calculateEndPadding(direction) + sides.end,
    bottom = calculateBottomPadding(),
)

/** Padding by [sides]'s room, its system bars and cutout consumed so a `Scaffold` inside doesn't keep clear of them again. */
internal fun Modifier.sidesPadding(
    sides: ContentSides,
): Modifier = padding(sides.padding).consumeWindowInsets(sides.insetPadding)

/** [content] of a layout that has applied the side room it sits in, so nothing inside applies it again. */
@Composable
internal fun SidesApplied(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentSides provides ContentSides.None, content = content)
}

/**
 * [content] shown apart from the screen that opens it, such as a sheet, dialog, menu or popup: it keeps none of the
 * screen's side room or room for the dock, which belong to the window behind it.
 */
@Composable
internal fun ApartFromScreen(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContentSides provides ContentSides.None,
        LocalDockClearance provides DockClearance.None,
        content = content,
    )
}
