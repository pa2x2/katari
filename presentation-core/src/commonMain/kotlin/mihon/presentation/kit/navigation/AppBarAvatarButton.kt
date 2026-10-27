package mihon.presentation.kit.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import mihon.presentation.kit.feedback.Tooltip

/** An app bar button showing an [avatar] instead of an icon, such as the active profile's; it ends the actions. */
@Composable
fun AppBarAvatarButton(
    contentDescription: String,
    onClick: () -> Unit,
    avatar: @Composable () -> Unit,
) {
    Tooltip(contentDescription) {
        androidx.compose.material3.IconButton(
            onClick = onClick,
            modifier = Modifier.semantics { this.contentDescription = contentDescription },
        ) {
            avatar()
        }
    }
}
