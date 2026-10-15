package mihon.presentation.kit.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.colors
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * A row of [Tab]s over pages. [scrollable] lets a long, user-defined list of tabs such as library categories scroll
 * instead of squeezing; [container] matches the surface the row sits on, such as a sheet.
 */
@Composable
fun TabRow(
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    container: SurfaceContainer = SurfaceContainer.Surface,
    divider: Boolean = true,
    tabs: @Composable () -> Unit,
) {
    val (fill, contentColor) = container.colors(KatariTheme.colors)
    val dividerContent: @Composable () -> Unit = if (divider) {
        { HorizontalDivider() }
    } else {
        {}
    }
    if (scrollable) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            modifier = modifier,
            containerColor = fill,
            contentColor = contentColor,
            edgePadding = 0.dp,
            divider = dividerContent,
            tabs = tabs,
        )
    } else {
        PrimaryTabRow(
            selectedTabIndex = selectedIndex,
            modifier = modifier,
            containerColor = fill,
            contentColor = contentColor,
            divider = dividerContent,
            tabs = tabs,
        )
    }
}

/** [count] adds a small pill after the title, such as the number of entries in a category. */
@Composable
fun Tab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    Tab(selected = selected, onClick = onClick, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        ) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (count != null) {
                Surface(container = SurfaceContainer.Highest, shape = KatariTheme.shapes.full) {
                    Text(
                        text = count.toString(),
                        modifier = Modifier.padding(horizontal = KatariTheme.spacing.xs + KatariTheme.spacing.xxs),
                        role = TextRole.Badge,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** A tab with its own label, such as a feed's source icon beside its name. */
@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
) {
    androidx.compose.material3.Tab(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        text = label,
        selectedContentColor = KatariTheme.colors.primary,
        unselectedContentColor = KatariTheme.colors.onSurfaceVariant,
    )
}
