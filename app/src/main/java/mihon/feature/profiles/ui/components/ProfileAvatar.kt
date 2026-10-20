package mihon.feature.profiles.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Lock
import mihon.presentation.kit.surface.Avatar
import mihon.presentation.kit.surface.AvatarSize
import mihon.presentation.kit.theme.LocalContentColor

/**
 * A profile's compact avatar from its [colorSeed]: the colour and face of its picker tile, with a lock badge when
 * [isLocked].
 */
@Composable
internal fun ProfileAvatar(
    colorSeed: Long,
    size: AvatarSize,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
) {
    Avatar(
        seed = colorSeed,
        size = size,
        modifier = modifier,
        badge = if (isLocked) MaterialSymbols.Rounded.Lock else null,
    ) {
        ProfileFace(modifier = Modifier.fillMaxSize(FACE_FRACTION).align(Alignment.Center))
    }
}

@Composable
internal fun ProfileFace(modifier: Modifier) {
    val faceColor = LocalContentColor.current
    Canvas(modifier = modifier) {
        val eyeRadius = size.minDimension * 0.05f
        drawCircle(faceColor, eyeRadius, Offset(size.width * 0.34f, size.height * 0.34f))
        drawCircle(faceColor, eyeRadius, Offset(size.width * 0.66f, size.height * 0.34f))
        drawArc(
            color = faceColor,
            startAngle = 18f,
            sweepAngle = 144f,
            useCenter = false,
            topLeft = Offset(size.width * 0.28f, size.height * 0.38f),
            size = Size(size.width * 0.44f, size.height * 0.28f),
            style = Stroke(width = size.minDimension * 0.045f, cap = StrokeCap.Round),
        )
    }
}

// Share of a compact avatar the face covers; picker tiles inset theirs with padding instead.
private const val FACE_FRACTION = 0.86f
