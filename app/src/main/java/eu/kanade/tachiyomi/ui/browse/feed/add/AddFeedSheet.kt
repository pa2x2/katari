package eu.kanade.tachiyomi.ui.browse.feed.add

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.source.model.BUILTIN_LATEST_PRESET_ID
import eu.kanade.domain.source.model.BUILTIN_POPULAR_PRESET_ID
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.presentation.browse.SourceUiModel
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.displayName
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.Verified
import mihon.icons.materialsymbols.roundedfilled.Favorite
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BrowseMetrics
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.field.SearchField
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Picks a source and one of its presets for a new feed in one sheet: a source row expands in place to list its
 * presets, marking the ones that already have a feed.
 */
@Composable
internal fun AddFeedSheet(
    sources: List<Source>,
    feeds: List<SourceFeed>,
    presetsFor: (Source) -> List<SourceFeedPreset>,
    onSelectPreset: (Source, SourceFeedPreset) -> Unit,
    onCreateFilteredFeed: (Source) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expandedSourceKey by rememberSaveable { mutableStateOf<String?>(null) }
    val listItems = remember(sources, query) { addFeedSourceList(sources, query) }
    val feedCounts = remember(feeds) { feeds.groupingBy { it.sourceId }.eachCount() }

    Sheet(onDismissRequest = onDismissRequest) {
        Column {
            SheetHeader(title = stringResource(MR.strings.browse_feed_add))
            SearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(MR.strings.browse_feed_search_sources),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.s),
            )
            ScrollbarLazyColumn {
                listItems.forEach { item ->
                    when (item) {
                        is SourceUiModel.Header -> item(key = "header-${item.language}") {
                            SourceSectionHeader(item.language)
                        }
                        is SourceUiModel.Item -> {
                            val source = item.source
                            val sourceKey = source.key()
                            val expanded = expandedSourceKey == sourceKey
                            item(key = sourceKey) {
                                AddFeedSourceRow(
                                    source = source,
                                    feedCount = feedCounts[source.id] ?: 0,
                                    expanded = expanded,
                                    onClick = { expandedSourceKey = if (expanded) null else sourceKey },
                                )
                            }
                            if (expanded) {
                                items(
                                    items = presetsFor(source),
                                    key = { "$sourceKey-preset-${it.id}" },
                                ) { preset ->
                                    AddFeedPresetRow(
                                        preset = preset,
                                        added = feeds.any { it.sourceId == source.id && it.presetId == preset.id },
                                        onClick = { onSelectPreset(source, preset) },
                                    )
                                }
                                item(key = "$sourceKey-filtered") {
                                    FilteredFeedRow(
                                        sourceName = source.name,
                                        onClick = { onCreateFilteredFeed(source) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceSectionHeader(language: String) {
    SectionHeader(text = LocaleHelper.getSourceDisplayName(language, LocalContext.current))
}

@Composable
private fun AddFeedSourceRow(
    source: Source,
    feedCount: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val arrowRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "expand")
    BaseSourceItem(
        source = source,
        onClickItem = onClick,
        action = {
            if (feedCount > 0) {
                Badge(text = pluralStringResource(MR.plurals.browse_feed_count, feedCount, feedCount))
            }
            Icon(
                imageVector = MaterialSymbols.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.rotate(arrowRotation),
            )
        },
    )
}

@Composable
private fun AddFeedPresetRow(
    preset: SourceFeedPreset,
    added: Boolean,
    onClick: () -> Unit,
) {
    SettingsRow(
        title = preset.displayName(),
        leading = { PresetIcon(preset.listingIcon()) },
        trailing = if (added) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                        tone = ContentTone.Primary,
                    )
                    Text(
                        text = stringResource(MR.strings.browse_feed_added),
                        role = TextRole.Supporting,
                        tone = ContentTone.Primary,
                    )
                }
            }
        } else {
            null
        },
        onClick = onClick,
    )
}

@Composable
private fun FilteredFeedRow(
    sourceName: String,
    onClick: () -> Unit,
) {
    SettingsRow(
        title = stringResource(MR.strings.browse_feed_filtered),
        subtitle = stringResource(MR.strings.browse_feed_filtered_summary, sourceName),
        leading = { PresetIcon(MaterialSymbols.Rounded.FilterList) },
        onClick = onClick,
    )
}

// Preset rows sit under their source's row, with the icon indented to the source name so they read as its children.
@Composable
private fun PresetIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.padding(start = BrowseMetrics.sourceIcon + KatariTheme.spacing.l),
        tone = ContentTone.Primary,
    )
}

private fun SourceFeedPreset.listingIcon(): ImageVector {
    return when (id) {
        BUILTIN_POPULAR_PRESET_ID -> MaterialSymbols.RoundedFilled.Favorite
        BUILTIN_LATEST_PRESET_ID -> MaterialSymbols.Rounded.Verified
        else -> MaterialSymbols.Rounded.FilterList
    }
}
