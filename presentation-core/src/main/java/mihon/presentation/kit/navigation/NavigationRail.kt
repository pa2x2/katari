package mihon.presentation.kit.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * Center-aligned M3 Navigation rail
 *
 * @see [androidx.compose.material3.NavigationRail]
 */
@Composable
fun NavigationRail(
    modifier: Modifier = Modifier,
    header: @Composable (ColumnScope.() -> Unit)? = null,
    windowInsets: WindowInsets = NavigationRailDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.material3.Surface(
        color = NavigationRailDefaults.ContainerColor,
        contentColor = KatariTheme.colors.onSurface,
        modifier = modifier,
        tonalElevation = 3.dp,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .windowInsetsPadding(windowInsets)
                .widthIn(min = 80.dp)
                .padding(vertical = KatariTheme.spacing.xs)
                .selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                KatariTheme.spacing.xs,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            if (header != null) {
                header()
                Spacer(Modifier.height(KatariTheme.spacing.s))
            }
            content()
        }
    }
}

@Composable
fun NavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = { Text(label, role = TextRole.Label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        alwaysShowLabel = true,
    )
}
