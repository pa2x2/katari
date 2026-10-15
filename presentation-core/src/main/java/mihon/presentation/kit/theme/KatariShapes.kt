package mihon.presentation.kit.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

/** Corner shape roles. */
@Immutable
class KatariShapes private constructor(
    val extraSmall: Shape,
    val small: Shape,
    val medium: Shape,
    val large: Shape,
    val extraLarge: Shape,
    /** [extraLarge] on the end corners only, for a tab tucked into the screen's start edge. */
    val extraLargeEnd: Shape,
    /** [extraLarge] on the start corners only, for a tab tucked into the screen's end edge. */
    val extraLargeStart: Shape,
    /** [extraLarge] on the top corners only, for a bar raised from the screen's bottom edge. */
    val extraLargeTop: Shape,
) {
    /** Pill or circle, depending on the element's aspect. */
    val full: Shape = CircleShape

    val none: Shape = RectangleShape

    internal companion object {
        @Composable
        @ReadOnlyComposable
        fun current(): KatariShapes {
            val shapes = MaterialTheme.shapes
            return KatariShapes(
                extraSmall = shapes.extraSmall,
                small = shapes.small,
                medium = shapes.medium,
                large = shapes.large,
                extraLarge = shapes.extraLarge,
                extraLargeEnd = shapes.extraLarge.withoutCorners(start = true),
                extraLargeStart = shapes.extraLarge.withoutCorners(start = false),
                extraLargeTop = shapes.extraLarge.copy(bottomStart = CornerSize(0), bottomEnd = CornerSize(0)),
            )
        }
    }
}

private fun CornerBasedShape.withoutCorners(start: Boolean): CornerBasedShape {
    val square = CornerSize(0)
    return if (start) copy(topStart = square, bottomStart = square) else copy(topEnd = square, bottomEnd = square)
}
