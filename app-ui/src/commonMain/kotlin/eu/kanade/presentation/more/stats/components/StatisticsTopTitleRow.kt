package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import app.katari.presentation.kit.content.CoverFrame
import app.katari.presentation.kit.content.StatisticsMetrics
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.content.entryCoverScale
import app.katari.presentation.kit.list.CoverRow
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.domain.entry.model.EntryCover as EntryCoverData

/**
 * One ranked title: cover, name, a bar of its share of [shareOf] in its type's [color], and its time with what it
 * finished under it. [rank] numbers the rows of a long list, such as See all; the dashboard's few rows leave it out.
 * Shared by the dashboard card, the See-all list and the Lifetime breakdown. A row [inRowGroup] is a cover row of a
 * row group card; otherwise it sits flush inside the padding of a statistics card.
 */
@Composable
internal fun StatisticsTopTitleRow(
    rank: Int?,
    title: String,
    cover: EntryCoverData,
    type: EntryType,
    completionCount: Long,
    durationMillis: Long,
    shareOf: Long,
    color: Color,
    formatDuration: (Long) -> String,
    onClick: (() -> Unit)?,
    inRowGroup: Boolean = false,
) {
    val share = if (shareOf > 0L) durationMillis.toFloat() / shareOf else 0f
    val finished = completionCount.takeIf { it > 0L }?.let { count ->
        pluralStringResource(type.entryTypePresentation().childCountPlural, count.toInt(), count.toInt())
    }
    val rankText: @Composable () -> Unit = {
        rank?.let {
            Text(
                text = it.toString(),
                modifier = Modifier.width(StatisticsMetrics.rankWidth),
                role = TextRole.Supporting,
            )
        }
    }
    val trailing: @Composable () -> Unit = {
        Column(Modifier.widthIn(min = StatisticsMetrics.rankedTrailingWidth), horizontalAlignment = Alignment.End) {
            Text(formatDuration(durationMillis), role = TextRole.Label, maxLines = 1)
            finished?.let { Text(text = it, role = TextRole.Supporting, maxLines = 1) }
        }
    }
    if (inRowGroup) {
        CoverRow(
            cover = {
                EntryCover.Book(data = cover, modifier = Modifier.fillMaxSize(), frame = CoverFrame.Thumbnail)
            },
            onClick = onClick,
            leading = if (rank != null) rankText else null,
            trailing = trailing,
        ) {
            TopTitleLines(title = title, share = share, color = color)
        }
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
    ) {
        rankText()
        EntryCover.Book(
            data = cover,
            modifier = Modifier.width(StatisticsMetrics.rankedCoverWidth * entryCoverScale()),
            frame = CoverFrame.Thumbnail,
        )
        Column(Modifier.weight(1f)) {
            TopTitleLines(title = title, share = share, color = color)
        }
        trailing()
    }
}

@Composable
private fun ColumnScope.TopTitleLines(title: String, share: Float, color: Color) {
    Text(text = title, role = TextRole.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
    StatisticsShareBar(
        fraction = share,
        color = color,
        modifier = Modifier.padding(top = KatariTheme.spacing.s),
        height = StatisticsMetrics.rankedShareBar,
    )
}
