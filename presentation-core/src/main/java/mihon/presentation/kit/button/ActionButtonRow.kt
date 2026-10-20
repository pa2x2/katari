package mihon.presentation.kit.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.color
import tachiyomi.presentation.core.components.material.ButtonColors
import tachiyomi.presentation.core.components.material.Button as ForkedButton

/**
 * A row of an item's actions, such as an entry's library, tracking and WebView actions, that scrolls when they don't
 * fit. Fill it with [ActionButton]s.
 */
@Composable
fun ActionButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = KatariTheme.spacing.contentInset),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        content = content,
    )
}

/**
 * One action of an [ActionButtonRow]: an outlined pill with its icon and label. Every action in the row shares this
 * treatment; one whose state is in effect, such as an entry already in the library, says so through [tone] and its
 * icon rather than a fill of its own.
 */
@Composable
fun ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ContentTone = ContentTone.Secondary,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = KatariTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    ForkedButton(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        interactionSource = interactionSource,
        elevation = null,
        shape = pressMorphShape(interactionSource),
        border = BorderStroke(KatariTheme.sizes.border, colors.outlineVariant),
        colors = ButtonColors(
            containerColor = Color.Transparent,
            contentColor = tone.color(fallback = colors.onSurfaceVariant),
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colors.disabledContent,
        ),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(KatariTheme.sizes.iconSmall))
        Spacer(Modifier.width(KatariTheme.spacing.s))
        Text(label, maxLines = 1)
    }
}
