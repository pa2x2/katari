package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.ChevronRight
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.LegendSwatch
import app.katari.presentation.kit.content.StatisticsMetrics
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.surface.DashboardCard
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.more.stats.data.StatsProgress
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The library's progress: one bar split into its stages, the stages that have titles with their counts, and, on the
 * Overview ([types] given), a line per type with its own bar that opens the type's tab.
 */
@Composable
internal fun StatisticsLibraryCard(
    progress: StatsProgress?,
    color: Color,
    titleCounts: Map<EntryType, Int>,
    progressByType: Map<EntryType, StatsProgress>,
    types: List<StatsType>,
    onTypeClick: (EntryType) -> Unit,
) {
    DashboardCard {
        if (progress == null) {
            Text(text = stringResource(MR.strings.not_applicable), tone = ContentTone.Secondary)
            return@DashboardCard
        }
        StatisticsProgressBar(
            progress = progress,
            color = color,
            height = StatisticsMetrics.stageBar,
            modifier = Modifier.clearAndSetSemantics {},
        )
        ProgressLegend(progress, color)
        if (progress.isPartial) {
            Text(
                text = stringResource(
                    MR.strings.statistics_progress_coverage,
                    progress.total,
                    progress.libraryTotal,
                ),
                modifier = Modifier.padding(top = KatariTheme.spacing.s),
                role = TextRole.Supporting,
            )
        }
        if (types.isNotEmpty()) {
            Column(Modifier.padding(top = KatariTheme.spacing.s)) {
                types.forEach { type ->
                    LibraryTypeLine(
                        label = stringResource(type.displayName),
                        count = titleCounts[type.type] ?: 0,
                        progress = progressByType[type.type],
                        color = type.accent.color(),
                        onClick = { onTypeClick(type.type) },
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ProgressLegend(progress: StatsProgress, color: Color) {
    val labels = listOf(
        stringResource(MR.strings.statistics_not_started),
        stringResource(MR.strings.statistics_in_progress),
        stringResource(MR.strings.statistics_caught_up),
        stringResource(MR.strings.completed),
    )
    val colors = statisticsProgressStageColors(color)
    val counts = progress.stageCounts()
    FlowRow(
        modifier = Modifier.padding(top = KatariTheme.spacing.m),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        // Most progressed first, as the bar reads from the empty end; stages without titles say nothing.
        labels.indices.reversed().filter { counts[it] > 0 }.forEach { index ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendSwatch(colors[index])
                Text(text = labels[index], role = TextRole.Supporting, maxLines = 1)
                Text(text = counts[index].toString(), role = TextRole.Label, maxLines = 1)
            }
        }
    }
}

@Composable
private fun LibraryTypeLine(
    label: String,
    count: Int,
    progress: StatsProgress?,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$label, $count" }
            .padding(vertical = KatariTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendSwatch(color)
        Text(text = label, modifier = Modifier.weight(LABEL_WEIGHT), role = TextRole.Label, maxLines = 1)
        if (progress != null) {
            StatisticsProgressBar(
                progress = progress,
                color = color,
                height = StatisticsMetrics.stageBarCompact,
                modifier = Modifier.weight(BAR_WEIGHT).clearAndSetSemantics {},
            )
        }
        Text(text = count.toString(), role = TextRole.Label)
        Icon(
            imageVector = MaterialSymbols.Rounded.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(KatariTheme.sizes.iconSmall),
            tone = ContentTone.Secondary,
        )
    }
}

// The bars of every line start at the same place and share one length, so the types compare at a glance.
private const val LABEL_WEIGHT = 1f
private const val BAR_WEIGHT = 2f
