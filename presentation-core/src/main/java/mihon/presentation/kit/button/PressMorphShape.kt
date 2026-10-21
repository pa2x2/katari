package mihon.presentation.kit.button

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * A pill that squares off while [interactionSource] is pressed, as Material's expressive buttons do. Material's own
 * morph is internal to its buttons, and the kit's buttons are forked from Material's to take long presses.
 */
@Composable
internal fun pressMorphShape(interactionSource: InteractionSource): Shape {
    val pressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "pressMorph",
    )
    val pressedCorner = with(LocalDensity.current) { PressedCorner.toPx() }
    return RoundedCornerShape(
        object : CornerSize {
            override fun toPx(shapeSize: Size, density: Density): Float =
                lerp(shapeSize.minDimension / 2f, pressedCorner, progress)
        },
    )
}

private val PressedCorner = 8.dp
