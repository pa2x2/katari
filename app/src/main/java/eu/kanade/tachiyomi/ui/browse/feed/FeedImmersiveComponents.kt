package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.components.AdaptiveSheet
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun FeedImmersivePill(
    source: Source,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.52f),
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceIcon(
                source = source,
                modifier = Modifier
                    .size(20.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun FeedPickerSheet(
    feeds: List<SourceFeed>,
    selectedFeedId: String,
    screenModel: FeedsScreenModel,
    canJumpToNewest: Boolean,
    onSelect: (String) -> Unit,
    onRefresh: (() -> Unit)?,
    onJumpToNewest: (() -> Unit)?,
    onAddFeed: () -> Unit,
    onManageFeeds: () -> Unit,
    onLongPressActions: () -> Unit,
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
            if (onRefresh != null) {
                FeedSheetAction(
                    label = stringResource(MR.strings.action_refresh),
                    icon = Icons.Outlined.Refresh,
                    onClick = onRefresh,
                )
            }
            if (canJumpToNewest && onJumpToNewest != null) {
                FeedSheetAction(
                    label = stringResource(MR.strings.action_move_to_top),
                    icon = Icons.Outlined.KeyboardArrowUp,
                    onClick = onJumpToNewest,
                )
            }
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(feeds, key = SourceFeed::id) { feed ->
                    val source = screenModel.sourceFor(feed.sourceId) ?: return@items
                    val preset = screenModel.presetFor(feed) ?: return@items
                    val selected = feed.id == selectedFeedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(feed.id) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SourceIcon(
                            source = source,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(MaterialTheme.shapes.small),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = source.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
            FeedSheetAction(
                label = stringResource(MR.strings.action_add),
                icon = Icons.Outlined.Add,
                onClick = onAddFeed,
            )
            FeedSheetAction(
                label = stringResource(MR.strings.browse_manage_feeds),
                icon = Icons.Outlined.DragHandle,
                onClick = onManageFeeds,
            )
            FeedSheetAction(
                label = stringResource(MR.strings.pref_browse_long_press_action_open_settings),
                icon = Icons.Outlined.Settings,
                onClick = onLongPressActions,
            )
        }
    }
}

@Composable
private fun FeedSheetAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
