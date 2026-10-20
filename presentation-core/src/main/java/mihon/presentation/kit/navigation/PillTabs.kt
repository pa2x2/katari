package mihon.presentation.kit.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * A scrolling row of pill tabs over pages whose list the user defines, such as library categories. The selected tab
 * is a filled pill, centred when it changes elsewhere, such as by swiping the pager. [tab] draws one [PillTab].
 */
@Composable
fun PillTabRow(
    count: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    tab: @Composable (index: Int) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        if (selectedIndex !in 0..<count) return@LaunchedEffect
        val layout = listState.layoutInfo
        val itemSize = layout.visibleItemsInfo.firstOrNull { it.index == selectedIndex }?.size ?: 0
        val viewport = layout.viewportEndOffset - layout.viewportStartOffset
        listState.animateScrollToItem(selectedIndex, scrollOffset = -(viewport - itemSize) / 2)
    }
    LazyRow(
        modifier = modifier.selectableGroup(),
        state = listState,
        contentPadding = PaddingValues(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        items(count) { index -> tab(index) }
    }
}

/** [count] follows the title, such as the number of entries in a category. */
@Composable
fun PillTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics {
            role = Role.Tab
            this.selected = selected
        },
        container = if (selected) SurfaceContainer.Primary else SurfaceContainer.High,
        shape = KatariTheme.shapes.full,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = PillTabHeight)
                .padding(horizontal = KatariTheme.spacing.l),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tone = if (selected) ContentTone.Inherit else ContentTone.Secondary
            Text(title, role = TextRole.Label, tone = tone, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (count != null) {
                Text(count.toString(), role = TextRole.Caption, tone = tone, maxLines = 1)
            }
        }
    }
}

private val PillTabHeight = 36.dp
