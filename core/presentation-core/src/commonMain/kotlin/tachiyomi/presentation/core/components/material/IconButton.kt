package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role

/**
 * Icon button with additional onLongClick functionality, for every style: [colors] and [border] make it standard,
 * filled, tonal or outlined.
 *
 * @see androidx.compose.material3.IconButton
 */
@Composable
internal fun IconButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLongClickLabel: String?,
    shape: Shape,
    colors: IconButtonColors,
    border: BorderStroke?,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val bordered = if (border != null) Modifier.border(border, shape) else Modifier
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(IconButtonDefaults.smallContainerSize())
            .clip(shape)
            .background(if (enabled) colors.containerColor else colors.disabledContainerColor, shape)
            .then(bordered)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}
