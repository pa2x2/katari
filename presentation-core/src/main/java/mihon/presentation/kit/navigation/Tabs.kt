package mihon.presentation.kit.navigation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.content.ProvideTextRole
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * A row of pill [Tab]s over a fixed set of pages, such as Browse's sections or a sheet's tabs. It scrolls when the
 * tabs don't fit, and brings the selected tab into view when it changes elsewhere, such as by swiping the pager.
 * [container] is the surface the row sits on, such as a sheet, so unselected tabs stand out from it.
 */
@Composable
fun TabRow(
    modifier: Modifier = Modifier,
    container: SurfaceContainer = SurfaceContainer.Surface,
    tabs: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTabContainer provides container.raised(),
        LocalTabBringsItselfIntoView provides true,
    ) {
        Row(
            modifier = modifier
                .selectableGroup()
                .horizontalScroll(rememberScrollState())
                .padding(TabRowPadding),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            tabs()
        }
    }
}

/**
 * A scrolling row of pill tabs over pages whose list the user defines, such as library categories. The selected tab
 * is centred when it changes elsewhere, such as by swiping the pager. [tab] draws one [Tab].
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
        contentPadding = TabRowPadding,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        items(count) { index -> tab(index) }
    }
}

/** [count] follows the title, such as the number of entries in a category. */
@Composable
fun Tab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    TabPill(selected = selected, onClick = onClick, modifier = modifier) {
        val tone = if (selected) ContentTone.Inherit else ContentTone.Secondary
        Text(title, role = TextRole.Label, tone = tone, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (count != null) {
            Text(count.toString(), role = TextRole.Caption, tone = tone, maxLines = 1)
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
    TabPill(selected = selected, onClick = onClick, modifier = modifier) {
        val content = if (selected) LocalContentColor.current else KatariTheme.colors.onSurfaceVariant
        CompositionLocalProvider(LocalContentColor provides content) {
            ProvideTextRole(TextRole.Label, label)
        }
    }
}

@Composable
private fun TabPill(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val bringsIntoView = LocalTabBringsItselfIntoView.current
    val requester = remember { BringIntoViewRequester() }
    if (bringsIntoView) {
        LaunchedEffect(selected) {
            if (selected) requester.bringIntoView()
        }
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .then(if (bringsIntoView) Modifier.bringIntoViewRequester(requester) else Modifier)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        container = if (selected) SurfaceContainer.Primary else LocalTabContainer.current,
        shape = KatariTheme.shapes.full,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = TabHeight)
                .padding(horizontal = KatariTheme.spacing.l),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

/** An unselected tab's fill: one step above the surface the row sits on. */
private fun SurfaceContainer.raised(): SurfaceContainer = when (this) {
    SurfaceContainer.High, SurfaceContainer.Highest -> SurfaceContainer.Highest
    else -> SurfaceContainer.High
}

private val LocalTabContainer = staticCompositionLocalOf { SurfaceContainer.High }

/**
 * Inside a [TabRow] the selected tab scrolls itself into view; [PillTabRow] scrolls by index instead, as its tabs
 * may not be composed yet.
 */
private val LocalTabBringsItselfIntoView = staticCompositionLocalOf { false }

private val TabRowPadding
    @Composable get() = PaddingValues(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.xs)

private val TabHeight = 36.dp
