package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import eu.kanade.presentation.more.stats.data.StatsProgress
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ChevronRight
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.LegendSwatch
import mihon.presentation.kit.content.StatisticsMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.PaneDivider
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Overview-only: each type's title count and progress, opening that type's tab. */
@Composable
internal fun StatisticsMediaCard(
    titleCounts: Map<EntryType, Int>,
    progressByType: Map<EntryType, StatsProgress>,
    types: List<StatsType>,
    onTypeClick: (EntryType) -> Unit,
) {
    StatisticsSectionCard(title = stringResource(MR.strings.statistics_by_media)) {
        types.forEachIndexed { index, type ->
            val count = titleCounts[type.type] ?: 0
            val label = stringResource(type.displayName)
            val color = type.accent.color()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTypeClick(type.type) }
                    .semantics { contentDescription = "$label, $count" }
                    .padding(vertical = KatariTheme.spacing.m),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                ) {
                    LegendSwatch(color)
                    Text(label, modifier = Modifier.weight(1f), role = TextRole.Body)
                    Text(count.toString(), role = TextRole.Label)
                    Icon(
                        imageVector = MaterialSymbols.Rounded.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                        tone = ContentTone.Secondary,
                    )
                }
                progressByType[type.type]?.let { progress ->
                    StatisticsProgressBar(
                        progress = progress,
                        color = color,
                        height = StatisticsMetrics.stageBarCompact,
                        modifier = Modifier
                            .padding(
                                start = KatariTheme.spacing.l,
                                top = KatariTheme.spacing.s,
                                end = KatariTheme.spacing.xl,
                            )
                            .clearAndSetSemantics {},
                    )
                }
            }
            if (index != types.lastIndex) PaneDivider()
        }
    }
}
