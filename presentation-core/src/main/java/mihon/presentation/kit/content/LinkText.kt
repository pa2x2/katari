package mihon.presentation.kit.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** Text that opens what it names, such as the store an extension comes from. Without [onClick] it reads as plain text. */
@Composable
fun LinkText(
    text: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    role: TextRole = TextRole.Label,
) {
    Text(
        text = text,
        modifier = modifier
            .clip(KatariTheme.shapes.small)
            .clickable(enabled = onClick != null, role = Role.Button) { onClick?.invoke() }
            .padding(vertical = KatariTheme.spacing.xxs),
        role = role,
        tone = if (onClick != null) ContentTone.Primary else ContentTone.Inherit,
        maxLines = 1,
    )
}
