package eu.kanade.tachiyomi.ui.browse.feed.switcher

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.tachiyomi.ui.browse.feed.FeedLabelText
import eu.kanade.tachiyomi.ui.browse.feed.FeedViewMode
import eu.kanade.tachiyomi.ui.browse.feed.rememberFeedLabel
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs

/**
 * The bar under a feed: previous and next, the feed switcher, and the view and display mode buttons. Swiping the
 * switcher sideways also moves to the previous or next feed, and long-pressing it renames the feed.
 */
@Composable
internal fun FeedNavigationBar(
    feeds: List<SourceFeed>,
    selectedFeed: SourceFeed,
    selectedDisplayMode: LibraryDisplayMode,
    sourceFor: (Long) -> Source?,
    presetFor: (SourceFeed) -> SourceFeedPreset?,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    immersiveAvailable: Boolean,
    onFeedViewModeChange: (FeedViewMode) -> Unit,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onFeedSelect: (String) -> Unit,
    onRenameFeed: () -> Unit,
    onAddFeed: () -> Unit,
    onManageFeeds: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedSource = sourceFor(selectedFeed.sourceId)
    val selectedPreset = presetFor(selectedFeed)
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = MaterialTheme.padding.small),
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.padding.small,
                    end = MaterialTheme.padding.small,
                    top = MaterialTheme.padding.small,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousClick, enabled = canGoPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(MR.strings.transition_previous),
                )
            }
            if (selectedSource != null && selectedPreset != null) {
                FeedSwitcher(
                    feeds = feeds,
                    selectedFeed = selectedFeed,
                    selectedSource = selectedSource,
                    selectedPreset = selectedPreset,
                    sourceFor = sourceFor,
                    presetFor = presetFor,
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = it },
                    onSwipePrevious = onPreviousClick.takeIf { canGoPrevious },
                    onSwipeNext = onNextClick.takeIf { canGoNext },
                    onFeedSelect = {
                        menuExpanded = false
                        onFeedSelect(it)
                    },
                    onRenameFeed = onRenameFeed,
                    onAddFeed = {
                        menuExpanded = false
                        onAddFeed()
                    },
                    onManageFeeds = {
                        menuExpanded = false
                        onManageFeeds()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            IconButton(onClick = onNextClick, enabled = canGoNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = stringResource(MR.strings.transition_next),
                )
            }
            IconButton(
                onClick = { onFeedViewModeChange(FeedViewMode.Immersive) },
                enabled = immersiveAvailable,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fullscreen,
                    contentDescription = stringResource(MR.strings.browse_feed_enter_immersive),
                )
            }
            FeedDisplayModeButton(
                displayMode = selectedDisplayMode,
                onDisplayModeChange = onDisplayModeChange,
            )
        }
    }
}

@Composable
private fun FeedSwitcher(
    feeds: List<SourceFeed>,
    selectedFeed: SourceFeed,
    selectedSource: Source,
    selectedPreset: SourceFeedPreset,
    sourceFor: (Long) -> Source?,
    presetFor: (SourceFeed) -> SourceFeedPreset?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSwipePrevious: (() -> Unit)?,
    onSwipeNext: (() -> Unit)?,
    onFeedSelect: (String) -> Unit,
    onRenameFeed: () -> Unit,
    onAddFeed: () -> Unit,
    onManageFeeds: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val swipeThreshold = with(density) { SwipeThreshold.toPx() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val currentOnSwipePrevious by rememberUpdatedState(onSwipePrevious)
    val currentOnSwipeNext by rememberUpdatedState(onSwipeNext)
    var anchorWidth by remember { mutableIntStateOf(0) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .onSizeChanged { anchorWidth = it.width }
                .clip(MaterialTheme.shapes.large)
                .pointerInput(swipeThreshold, rtl) {
                    var dragged = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = {
                            // Swiping towards the start reveals the next feed, as with pages.
                            val towardsStart = if (rtl) dragged > 0 else dragged < 0
                            if (abs(dragged) >= swipeThreshold) {
                                if (towardsStart) currentOnSwipeNext?.invoke() else currentOnSwipePrevious?.invoke()
                            }
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragged += amount
                        },
                    )
                }
                .combinedClickable(
                    onClick = { onExpandedChange(true) },
                    onLongClick = onRenameFeed,
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
                FeedSourceIcon(selectedSource, size = 24.dp)
                FeedLabelText(
                    label = rememberFeedLabel(selectedFeed, selectedSource, selectedPreset),
                    titleStyle = MaterialTheme.typography.labelLarge,
                    subtitleColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
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
internal fun FeedSourceIcon(source: Source, size: Dp) {
    SourceIcon(
        source = source,
        modifier = Modifier
            .size(size)
            .clip(MaterialTheme.shapes.extraSmall),
    )
}

private val SwipeThreshold = 48.dp
