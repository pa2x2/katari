package mihon.presentation.kit.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import androidx.compose.material3.NavigationBarItem as MaterialNavigationBarItem

/**
 * M3 Navbar with no horizontal spacer
 *
 * @see [androidx.compose.material3.NavigationBar]
 */
@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    content: @Composable RowScope.() -> Unit,
) {
    androidx.compose.material3.Surface(
        color = NavigationBarDefaults.containerColor,
        contentColor = KatariTheme.colors.onSurface,
        tonalElevation = NavigationBarDefaults.Elevation,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets)
                .height(80.dp)
                .selectableGroup(),
            content = content,
        )
    }
}

/** [icon] is a slot so a destination can animate its icon or carry a [mihon.presentation.kit.content.NotificationBadge]. */
@Composable
fun RowScope.NavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialNavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = { NavigationLabel(label) },
        modifier = modifier,
        alwaysShowLabel = true,
    )
}

@Composable
internal fun NavigationLabel(label: String) {
    Text(label, role = TextRole.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
