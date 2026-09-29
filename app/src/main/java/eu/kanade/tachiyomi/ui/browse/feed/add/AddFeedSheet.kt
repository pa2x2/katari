package eu.kanade.tachiyomi.ui.browse.feed.add

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.model.BUILTIN_LATEST_PRESET_ID
import eu.kanade.domain.source.model.BUILTIN_POPULAR_PRESET_ID
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.presentation.browse.SourceUiModel
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.displayName
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.header

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

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(top = MaterialTheme.padding.medium)) {
            Text(
                text = stringResource(MR.strings.browse_feed_add),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
                placeholder = { Text(text = stringResource(MR.strings.browse_feed_search_sources)) },
                leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
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
    val context = LocalContext.current
    Text(
        text = LocaleHelper.getSourceDisplayName(language, context),
        modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
        style = MaterialTheme.typography.header,
    )
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
        modifier = Modifier.padding(vertical = MaterialTheme.padding.extraSmall),
        onClickItem = onClick,
        action = {
            if (feedCount > 0) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(start = MaterialTheme.padding.small),
                ) {
                    Text(
                        text = pluralStringResource(MR.plurals.browse_feed_count, feedCount, feedCount),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = MaterialTheme.padding.small)
                    .rotate(arrowRotation),
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
    PresetRow(
        icon = preset.listingIcon(),
        onClick = onClick,
        content = {
            Text(
                text = preset.displayName(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (added) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(MR.strings.browse_feed_added),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

@Composable
private fun FilteredFeedRow(
    sourceName: String,
    onClick: () -> Unit,
) {
    PresetRow(
        icon = Icons.Outlined.FilterList,
        onClick = onClick,
        content = {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(MR.strings.browse_feed_filtered))
                Text(
                    text = stringResource(MR.strings.browse_feed_filtered_summary, sourceName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun PresetRow(
    icon: ImageVector,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 72.dp, end = MaterialTheme.padding.medium, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        content()
    }
}

private fun SourceFeedPreset.listingIcon(): ImageVector {
    return when (id) {
        BUILTIN_POPULAR_PRESET_ID -> Icons.Outlined.Favorite
        BUILTIN_LATEST_PRESET_ID -> Icons.Outlined.NewReleases
        else -> Icons.Outlined.FilterList
    }
}
