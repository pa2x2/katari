package app.katari.presentation.kit.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import app.katari.presentation.kit.content.InlineIcon
import app.katari.presentation.kit.feedback.HoverTooltip
import app.katari.presentation.kit.pointer.clickCursor
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

/**
 * A button inside a line of [role] text, drawn at the text's size, such as the sync mark after a setting's title that
 * opens the setting's sync choice. The line keeps its height: a touch anywhere in the standard touch target around the
 * icon presses it, as Compose gives a small button that reach, and the press shows over that target. In a window worked
 * with a mouse, hovering it shows [contentDescription] as its tooltip.
 */
@Composable
fun InlineIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    role: TextRole,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ContentTone = ContentTone.Inherit,
) {
    HoverTooltip(contentDescription) {
        Box(
            modifier = modifier
                .clickCursor()
                .clickable(
                    interactionSource = null,
                    indication = ripple(bounded = false, radius = KatariTheme.sizes.touchTarget / 2),
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            InlineIcon(imageVector, contentDescription, role, tone = tone)
        }
    }
}
