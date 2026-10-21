package eu.kanade.tachiyomi.ui.browse.feed.switcher

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.tachiyomi.ui.browse.feed.FeedLabel
import eu.kanade.tachiyomi.ui.browse.feed.FeedLabelText
import eu.kanade.tachiyomi.ui.browse.feed.rememberFeedLabel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.DragHandle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.LogoSize
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.menu.MenuPill
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The selected feed as a chip that follows the finger when swiped sideways, bringing in the previous or next feed.
 * Tapping it opens the feed menu and long-pressing it renames the feed.
 */
@Composable
internal fun FeedSwitcher(
    feeds: List<SourceFeed>,
    selectedFeed: SourceFeed,
    sourceFor: (Long) -> Source?,
    presetFor: (SourceFeed) -> SourceFeedPreset?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onFeedSelect: (String) -> Unit,
    onRenameFeed: () -> Unit,
    onAddFeed: () -> Unit,
    onManageFeeds: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val selectedIndex = feeds.indexOfFirst { it.id == selectedFeed.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = selectedIndex) { feeds.size }
    val feedIds = feeds.map { it.id }
    var pagedFeedIds by remember { mutableStateOf(feedIds) }
    val currentFeeds by rememberUpdatedState(feeds)
    val currentSelectedFeedId by rememberUpdatedState(selectedFeed.id)
    val currentOnFeedSelect by rememberUpdatedState(onFeedSelect)
    var anchorWidth by remember { mutableIntStateOf(0) }

    // Follows a selection made elsewhere, sliding to it, or jumps when the feed list itself changed so the pager
    // doesn't sweep across feeds that now sit at different positions.
    LaunchedEffect(selectedIndex, feedIds) {
        val listChanged = feedIds != pagedFeedIds
        pagedFeedIds = feedIds
        when {
            pagerState.currentPage == selectedIndex -> Unit
            listChanged -> pagerState.scrollToPage(selectedIndex)
            else -> pagerState.animateScrollToPage(selectedIndex)
        }
    }
    // Only a swipe selects the feed the pager comes to rest on; the pager also settles after following a selection
    // or after clamping to a shorter list, and those must not override the selection.
    LaunchedEffect(pagerState) {
        var swiped = false
        launch {
            pagerState.interactionSource.interactions.collect {
                if (it is DragInteraction.Start) swiped = true
            }
        }
        snapshotFlow { pagerState.isScrollInProgress }
            .filter { !it }
            .collect {
                if (!swiped) return@collect
                swiped = false
                val feed = currentFeeds.getOrNull(pagerState.currentPage) ?: return@collect
                if (feed.id != currentSelectedFeedId) currentOnFeedSelect(feed.id)
            }
    }

    Box(modifier = modifier.onSizeChanged { anchorWidth = it.width }) {
        // Swiping past the first or last feed stretches the switcher instead of moving the Browse tabs.
        val edgeOverscroll = rememberEdgeContainedOverscroll()
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.nestedScroll(edgeOverscroll),
            overscrollEffect = edgeOverscroll,
            pageSpacing = KatariTheme.spacing.s,
            key = { feeds[it].id },
        ) { page ->
            val feed = feeds[page]
            val source = sourceFor(feed.sourceId) ?: return@HorizontalPager
            val preset = presetFor(feed) ?: return@HorizontalPager
            FeedSwitcherChip(
                label = rememberFeedLabel(feed, source, preset),
                source = source,
                onClick = { onExpandedChange(true) },
                onLongClick = onRenameFeed,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.width(with(density) { anchorWidth.toDp() }),
        ) {
            feeds.forEach { feed ->
                val source = sourceFor(feed.sourceId) ?: return@forEach
                val preset = presetFor(feed) ?: return@forEach
                val label = rememberFeedLabel(feed, source, preset)
                DropdownMenuItem(
                    label = label.title,
                    supportingLabel = label.subtitle,
                    onClick = { onFeedSelect(feed.id) },
                    leading = { SourceIcon(source = source, size = LogoSize.Icon) },
                    trailing = if (feed.id == selectedFeed.id) {
                        {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Check,
                                contentDescription = stringResource(MR.strings.selected),
                                tone = ContentTone.Primary,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                label = stringResource(MR.strings.browse_feed_add),
                onClick = onAddFeed,
                icon = MaterialSymbols.Rounded.Add,
            )
            DropdownMenuItem(
                label = stringResource(MR.strings.browse_manage_feeds),
                onClick = onManageFeeds,
                icon = MaterialSymbols.Rounded.DragHandle,
            )
        }
    }
}

@Composable
private fun FeedSwitcherChip(
    label: FeedLabel,
    source: Source,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    MenuPill(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        onLongClick = onLongClick,
        onLongClickLabel = stringResource(MR.strings.browse_feed_rename),
        leading = { SourceIcon(source = source, size = LogoSize.Icon) },
    ) {
        FeedLabelText(
            label = label,
            titleRole = TextRole.Label,
            subtitleTone = ContentTone.Inherit,
            modifier = Modifier.weight(1f),
        )
    }
}
