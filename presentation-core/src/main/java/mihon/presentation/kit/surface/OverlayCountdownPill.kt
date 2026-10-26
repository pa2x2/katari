package mihon.presentation.kit.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.theme.KatariTheme

/**
 * A pill over media for an action that runs by itself once [progress] (0..1) fills it, such as a skip that happens
 * unless stopped. The fill sweeps behind [content] from the start; tapping the pill runs the action at once
 * ([onClick]) and the close button at its end stops the countdown ([onDismiss]). [progress] is read while drawing, so
 * a countdown redraws the pill without recomposing it.
 */
@Composable
fun OverlayCountdownPill(
    progress: () -> Float,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val fill = KatariTheme.colors.onMedia.copy(alpha = COUNTDOWN_FILL_ALPHA)
    val divider = KatariTheme.colors.onMedia.copy(alpha = DIVIDER_ALPHA)
    Surface(modifier = modifier, container = SurfaceContainer.MediaScrim, shape = KatariTheme.shapes.full) {
        Row(modifier = Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .drawBehind {
                        drawRect(fill, size = size.copy(width = size.width * progress().coerceIn(0f, 1f)))
                    }
                    .clickable(role = Role.Button, onClick = onClick),
            ) {
                content()
            }
            Box(
                modifier = Modifier
                    .width(DividerThickness)
                    .fillMaxHeight()
                    .padding(vertical = KatariTheme.spacing.s)
                    .background(divider),
            )
            Box(
                modifier = Modifier
                    .clickable(role = Role.Button, onClickLabel = dismissLabel, onClick = onDismiss)
                    .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Close,
                    contentDescription = dismissLabel,
                    modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                )
            }
        }
    }
}

private val DividerThickness = 1.dp

private const val COUNTDOWN_FILL_ALPHA = 0.28f
private const val DIVIDER_ALPHA = 0.32f
