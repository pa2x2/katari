package eu.kanade.tachiyomi.ui.browse.feed.switcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.tachiyomi.ui.browse.feed.FeedViewMode
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The bar under a feed: the feed switcher and the view and display mode buttons.
 */
@Composable
internal fun FeedNavigationBar(
    feeds: List<SourceFeed>,
    selectedFeed: SourceFeed,
    selectedDisplayMode: LibraryDisplayMode,
    sourceFor: (Long) -> Source?,
    presetFor: (SourceFeed) -> SourceFeedPreset?,
    immersiveAvailable: Boolean,
    onFeedViewModeChange: (FeedViewMode) -> Unit,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
    onFeedSelect: (String) -> Unit,
    onRenameFeed: () -> Unit,
    onAddFeed: () -> Unit,
    onManageFeeds: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            FeedSwitcher(
                feeds = feeds,
                selectedFeed = selectedFeed,
                sourceFor = sourceFor,
                presetFor = presetFor,
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it },
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
