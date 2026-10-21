package eu.kanade.tachiyomi.ui.browse.feed.switcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.tachiyomi.ui.browse.feed.FeedViewMode
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Fullscreen
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.navigation.FloatingToolbar
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The feed switcher and the view and display mode buttons, floating over the bottom of the feed's grid. */
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

    FloatingToolbar(modifier = modifier) {
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
