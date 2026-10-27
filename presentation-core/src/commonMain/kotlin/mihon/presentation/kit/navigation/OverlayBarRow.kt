package mihon.presentation.kit.navigation

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * An action that continues an overlay app bar on a row beneath it, in the bar's own colour, such as Search all chapters
 * under a reader's search field. [value] sits at the row's end.
 */
@Composable
fun OverlayBarRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
) {
    SettingsRow(
        title = title,
        icon = icon,
        onClick = onClick,
        role = Role.Button,
        trailing = value?.let { { Text(it, role = TextRole.Supporting, tone = ContentTone.Secondary) } },
        modifier = modifier.background(KatariTheme.colors.chrome),
    )
}
