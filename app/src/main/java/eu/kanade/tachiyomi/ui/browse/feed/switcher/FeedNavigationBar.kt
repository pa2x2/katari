package eu.kanade.tachiyomi.ui.browse.feed.switcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.tachiyomi.ui.browse.feed.FeedViewMode
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Fullscreen
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The bar under a feed: the feed switcher and the view and display mode buttons. [bottomPadding] keeps the buttons
 * clear of whatever covers the screen's bottom edge, such as the home dock, while the bar's fill runs on under it.
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
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(KatariTheme.colors.surface)
            .padding(bottom = bottomPadding + KatariTheme.spacing.s),
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = KatariTheme.spacing.s,
                    end = KatariTheme.spacing.s,
                    top = KatariTheme.spacing.s,
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
                icon = MaterialSymbols.Rounded.Fullscreen,
                contentDescription = stringResource(MR.strings.browse_feed_enter_immersive),
                onClick = { onFeedViewModeChange(FeedViewMode.Immersive) },
                enabled = immersiveAvailable,
            )
            FeedDisplayModeButton(
                displayMode = selectedDisplayMode,
                onDisplayModeChange = onDisplayModeChange,
            )
        }
    }
}
