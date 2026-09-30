package eu.kanade.tachiyomi.ui.browse.feed.switcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.ui.browse.feed.FeedLabelText
import eu.kanade.tachiyomi.ui.browse.feed.rememberFeedLabel
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The feed switcher shown from the context chip in immersive mode, with the actions the feed bar would offer. */
@Composable
internal fun FeedPickerSheet(
    feeds: List<SourceFeed>,
    selectedFeedId: String,
    sourceFor: (Long) -> Source?,
    presetFor: (SourceFeed) -> SourceFeedPreset?,
    canJumpToNewest: Boolean,
    onSelect: (String) -> Unit,
    onRefresh: () -> Unit,
    onJumpToNewest: () -> Unit,
    onAddFeed: () -> Unit,
    onManageFeeds: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = stringResource(MR.strings.browse_feeds),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            FeedSheetAction(
                label = stringResource(MR.strings.action_refresh),
                icon = Icons.Outlined.Refresh,
                onClick = onRefresh,
            )
            if (canJumpToNewest) {
                FeedSheetAction(
                    label = stringResource(MR.strings.action_move_to_top),
                    icon = Icons.Outlined.KeyboardArrowUp,
                    onClick = onJumpToNewest,
                )
            }
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(feeds, key = SourceFeed::id) { feed ->
                    val source = sourceFor(feed.sourceId) ?: return@items
                    val preset = presetFor(feed) ?: return@items
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(feed.id) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FeedSourceIcon(source, size = 32.dp)
                        FeedLabelText(
                            label = rememberFeedLabel(feed, source, preset),
                            modifier = Modifier.weight(1f),
                        )
                        if (feed.id == selectedFeedId) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
            FeedSheetAction(
                label = stringResource(MR.strings.browse_feed_add),
                icon = Icons.Outlined.Add,
                onClick = onAddFeed,
            )
            FeedSheetAction(
                label = stringResource(MR.strings.browse_manage_feeds),
                icon = Icons.Outlined.DragHandle,
                onClick = onManageFeeds,
            )
        }
    }
}

@Composable
private fun FeedSheetAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
