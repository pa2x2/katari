package eu.kanade.tachiyomi.ui.browse.feed.switcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.tachiyomi.ui.browse.feed.rememberFeedLabel
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.DragHandle
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.LogoSize
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
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
    Sheet(onDismissRequest = onDismissRequest, capHeight = true) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(title = stringResource(MR.strings.browse_feeds))
            RowGroup {
                row {
                    SettingsRow(
                        title = stringResource(MR.strings.action_refresh),
                        icon = MaterialSymbols.Rounded.Refresh,
                        onClick = onRefresh,
                    )
                }
                if (canJumpToNewest) {
                    row {
                        SettingsRow(
                            title = stringResource(MR.strings.action_move_to_top),
                            icon = MaterialSymbols.Rounded.KeyboardArrowUp,
                            onClick = onJumpToNewest,
                        )
                    }
                }
            }
            RowGroupGap()
            // Feeds whose source or preset is gone are left out until they come back.
            val shownFeeds = feeds.mapNotNull { feed ->
                val source = sourceFor(feed.sourceId) ?: return@mapNotNull null
                val preset = presetFor(feed) ?: return@mapNotNull null
                Triple(feed, source, preset)
            }
            RowList(modifier = Modifier.weight(1f, fill = false)) {
                itemsIndexed(shownFeeds, key = { _, (feed) -> feed.id }) { index, (feed, source, preset) ->
                    RowGroupItem(position = RowGroupPosition.of(index, shownFeeds.size)) {
                        val label = rememberFeedLabel(feed, source, preset)
                        SettingsRow(
                            title = label.title,
                            subtitle = label.subtitle,
                            subtitleMaxLines = 1,
                            leading = { SourceIcon(source = source, size = LogoSize.Compact) },
                            trailing = if (feed.id == selectedFeedId) {
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
                            onClick = { onSelect(feed.id) },
                        )
                    }
                }
            }
            RowGroupGap()
            RowGroup {
                row {
                    SettingsRow(
                        title = stringResource(MR.strings.browse_feed_add),
                        icon = MaterialSymbols.Rounded.Add,
                        onClick = onAddFeed,
                    )
                }
                row {
                    SettingsRow(
                        title = stringResource(MR.strings.browse_manage_feeds),
                        icon = MaterialSymbols.Rounded.DragHandle,
                        onClick = onManageFeeds,
                    )
                }
            }
            RowGroupGap()
        }
    }
}
