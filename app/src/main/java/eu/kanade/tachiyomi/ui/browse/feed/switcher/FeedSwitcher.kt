package eu.kanade.tachiyomi.ui.browse.feed.switcher

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.tachiyomi.ui.browse.feed.FeedLabel
import eu.kanade.tachiyomi.ui.browse.feed.FeedLabelText
import eu.kanade.tachiyomi.ui.browse.feed.rememberFeedLabel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.padding
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
        HorizontalPager(
            state = pagerState,
            // Swiping past the first or last feed stays on the switcher instead of moving the Browse tabs.
            modifier = Modifier.nestedScroll(ConsumeHorizontalScroll),
            pageSpacing = MaterialTheme.padding.small,
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
                DropdownMenuItem(
                    text = {
                        FeedLabelText(
                            label = rememberFeedLabel(feed, source, preset),
                            titleStyle = LocalTextStyle.current,
                        )
                    },
                    onClick = { onFeedSelect(feed.id) },
                    leadingIcon = { FeedSourceIcon(source, size = 24.dp) },
                    trailingIcon = if (feed.id == selectedFeed.id) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(MR.strings.selected),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(text = stringResource(MR.strings.browse_feed_add)) },
                onClick = onAddFeed,
                leadingIcon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(MR.strings.browse_manage_feeds)) },
                onClick = onManageFeeds,
                leadingIcon = { Icon(imageVector = Icons.Outlined.DragHandle, contentDescription = null) },
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = stringResource(MR.strings.browse_feed_rename),
            ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedSourceIcon(source, size = 24.dp)
            FeedLabelText(
                label = label,
                titleStyle = MaterialTheme.typography.labelLarge,
                subtitleColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = null)
        }
    }
}

@Composable
internal fun FeedSourceIcon(source: Source, size: Dp) {
    SourceIcon(
        source = source,
        modifier = Modifier
            .size(size)
            .clip(MaterialTheme.shapes.extraSmall),
    )
}

private val ConsumeHorizontalScroll = object : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        return available.copy(y = 0f)
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return available.copy(y = 0f)
    }
}
