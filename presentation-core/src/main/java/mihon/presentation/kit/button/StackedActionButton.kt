package mihon.presentation.kit.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.theme.color
import tachiyomi.presentation.core.components.material.ButtonColors
import tachiyomi.presentation.core.components.material.TextButton

/**
 * A text button with its icon above the label, for a row of equally weighted actions such as share and save.
 * [tone] colours an action by its state, such as an entry already in the library; [dense] fits five or more of them
 * side by side with a smaller icon and label.
 */
@Composable
fun StackedActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ContentTone = ContentTone.Primary,
    dense: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = KatariTheme.colors
    TextButton(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        shape = KatariTheme.shapes.full,
        colors = ButtonColors(
            containerColor = Color.Transparent,
            contentColor = tone.color(fallback = colors.primary),
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colors.disabledContent,
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = if (dense) Modifier.size(KatariTheme.sizes.iconSmall) else Modifier,
            )
            Text(text = label, role = if (dense) TextRole.Badge else null, textAlign = TextAlign.Center)
        }
    }
}
