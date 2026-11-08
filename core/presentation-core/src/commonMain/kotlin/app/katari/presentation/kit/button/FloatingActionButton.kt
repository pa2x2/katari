package app.katari.presentation.kit.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.progress.LoadingIndicator
import app.katari.presentation.kit.theme.KatariTheme
import androidx.compose.material3.SmallFloatingActionButton as MaterialSmallFloatingActionButton

/**
 * The screen's floating action; collapses to its icon when not [expanded], such as while the list scrolls. It
 * animates out of and back into the screen's end corner as [visible] changes, such as when it applies only to a
 * selection. [loading] shows the wait indicator in place of [icon] while what the action opens is still being
 * checked; the action works meanwhile.
 */
@Composable
fun FloatingActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    visible: Boolean = true,
    loading: Boolean = false,
) {
    SmallExtendedFloatingActionButton(
        text = { Text(label) },
        icon = {
            if (loading) {
                LoadingIndicator(Modifier.size(KatariTheme.sizes.icon))
            } else {
                Icon(icon, contentDescription = null)
            }
        },
        onClick = onClick,
        modifier = modifier.animateFloatingActionButton(visible = visible, alignment = Alignment.BottomEnd),
        expanded = expanded,
    )
}

/**
 * A small icon-only floating action stacked above the screen's main one, for a secondary jump such as back to the
 * current chapter. It animates in and out like [FloatingActionButton].
 */
@Composable
fun SmallFloatingActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    MaterialSmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier.animateFloatingActionButton(visible = visible, alignment = Alignment.BottomEnd),
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
