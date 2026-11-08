package eu.kanade.tachiyomi.ui.browse.feed.add

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import app.katari.core.platform.currentPlatformContext
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Check
import app.katari.icons.materialsymbols.rounded.FilterList
import app.katari.icons.materialsymbols.rounded.KeyboardArrowDown
import app.katari.icons.materialsymbols.rounded.Verified
import app.katari.icons.materialsymbols.roundedfilled.Favorite
import app.katari.presentation.kit.content.Badge
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.InlineIcon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.field.SearchField
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.sheet.Sheet
import app.katari.presentation.kit.sheet.SheetHeader
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.domain.source.interactor.SourceListUiMapper
import eu.kanade.domain.source.model.BUILTIN_LATEST_PRESET_ID
import eu.kanade.domain.source.model.BUILTIN_POPULAR_PRESET_ID
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.displayName
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
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
    val sourceGroups = remember(sources, query) { SourceListUiMapper.map(sources, query).groups }
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
                    .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
            )
            val rows = buildList {
                sourceGroups.forEach { group ->
                    add(AddFeedRow.Language(group.language))
                    group.sources.forEach { source ->
                        val expanded = expandedSourceKey == source.key()
                        add(AddFeedRow.SourceRow(source, expanded))
                        if (expanded) {
                            presetsFor(source).forEach { add(AddFeedRow.Preset(source, it)) }
                            add(AddFeedRow.Filtered(source))
                        }
                    }
                }
            }
            // Each language is a card of its sources, with an open source's feeds under it in the same card.
            val positions = RowGroupPosition.ofRuns(rows) { it !is AddFeedRow.Language }
            RowList {
                rows.forEachIndexed { index, row ->
                    item(key = row.key) {
                        when (row) {
                            is AddFeedRow.Language -> SourceSectionHeader(row.language)
                            is AddFeedRow.SourceRow -> RowGroupItem(position = checkNotNull(positions[index])) {
                                val sourceKey = row.source.key()
                                AddFeedSourceRow(
                                    source = row.source,
                                    feedCount = feedCounts[row.source.id] ?: 0,
                                    expanded = row.expanded,
                                    onClick = { expandedSourceKey = if (row.expanded) null else sourceKey },
                                )
                            }
                            is AddFeedRow.Preset -> RowGroupItem(
                                position = checkNotNull(positions[index]),
                                continuesAbove = true,
                            ) {
                                AddFeedPresetRow(
                                    preset = row.preset,
                                    added = feeds.any { it.sourceId == row.source.id && it.presetId == row.preset.id },
                                    onClick = { onSelectPreset(row.source, row.preset) },
                                )
                            }
                            is AddFeedRow.Filtered -> RowGroupItem(
                                position = checkNotNull(positions[index]),
                                continuesAbove = true,
                            ) {
                                FilteredFeedRow(
                                    sourceName = row.source.name,
                                    onClick = { onCreateFilteredFeed(row.source) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface AddFeedRow {
    val key: String

    data class Language(val language: String) : AddFeedRow {
        override val key get() = "header-$language"
    }

    data class SourceRow(val source: Source, val expanded: Boolean) : AddFeedRow {
        override val key get() = source.key()
    }

    data class Preset(val source: Source, val preset: SourceFeedPreset) : AddFeedRow {
        override val key get() = "${source.key()}-preset-${preset.id}"
    }

    data class Filtered(val source: Source) : AddFeedRow {
        override val key get() = "${source.key()}-filtered"
    }
}

@Composable
private fun SourceSectionHeader(language: String) {
    RowGroupHeader(text = LocaleHelper.getSourceDisplayName(language, currentPlatformContext()))
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
        nested = true,
        trailing = if (added) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InlineIcon(
                        imageVector = MaterialSymbols.Rounded.Check,
                        contentDescription = null,
                        role = TextRole.Supporting,
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
        nested = true,
    )
}

@Composable
private fun PresetIcon(icon: ImageVector) {
    Icon(imageVector = icon, contentDescription = null, tone = ContentTone.Primary)
}

private fun SourceFeedPreset.listingIcon(): ImageVector {
    return when (id) {
        BUILTIN_POPULAR_PRESET_ID -> MaterialSymbols.RoundedFilled.Favorite
        BUILTIN_LATEST_PRESET_ID -> MaterialSymbols.Rounded.Verified
        else -> MaterialSymbols.Rounded.FilterList
    }
}
