package mihon.entry.interactions.book.reader.layout

import android.os.Build
import android.view.RoundedCorner
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import mihon.entry.interactions.book.reader.BookReaderProgressInsets
import kotlin.math.max
import kotlin.math.roundToInt

/** Keeps edge-bound reader overlays within the rectangular safe area of physical rounded corners. */
@Composable
internal fun BookReaderRoundedCornerSafeArea(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(BookReaderProgressInsets) -> Unit,
) {
    val view = LocalView.current
    var insets by remember(view) { mutableStateOf(BookReaderProgressInsets.Zero) }

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            val updated = view.roundedCornerPadding(coordinates)
            if (updated != insets) insets = updated
        },
        content = { content(insets) },
    )
}

private fun View.roundedCornerPadding(coordinates: LayoutCoordinates): BookReaderProgressInsets {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return BookReaderProgressInsets.Zero
    val insets = rootWindowInsets ?: return BookReaderProgressInsets.Zero
    val root = rootView
    if (root.width <= 0 || root.height <= 0) return BookReaderProgressInsets.Zero

    val topLeft = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
    val topRight = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0
    val bottomLeft = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
    val bottomRight = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0
    val position = coordinates.positionInWindow()
    val size = coordinates.size
    return roundedCornerPadding(
        position = position,
        width = size.width,
        height = size.height,
        windowWidth = root.width,
        windowHeight = root.height,
        leftRadius = max(topLeft, bottomLeft),
        topRadius = max(topLeft, topRight),
        rightRadius = max(topRight, bottomRight),
        bottomRadius = max(bottomLeft, bottomRight),
    )
}

private fun roundedCornerPadding(
    position: Offset,
    width: Int,
    height: Int,
    windowWidth: Int,
    windowHeight: Int,
    leftRadius: Int,
    topRadius: Int,
    rightRadius: Int,
    bottomRadius: Int,
): BookReaderProgressInsets {
    val leftMargin = position.x.roundToInt().coerceAtLeast(0)
    val topMargin = position.y.roundToInt().coerceAtLeast(0)
    val rightMargin = (windowWidth - position.x - width).roundToInt().coerceAtLeast(0)
    val bottomMargin = (windowHeight - position.y - height).roundToInt().coerceAtLeast(0)
    return BookReaderProgressInsets(
        left = (leftRadius - leftMargin).coerceAtLeast(0),
        top = (topRadius - topMargin).coerceAtLeast(0),
        right = (rightRadius - rightMargin).coerceAtLeast(0),
        bottom = (bottomRadius - bottomMargin).coerceAtLeast(0),
    )
}
