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
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.StatisticsMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.CoverRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt
import tachiyomi.domain.entry.model.EntryCover as EntryCoverData

/**
 * One ranked title: cover, optional type label, completed children, time and its share of [shareOf].
 * Shared by the dashboard card, the See-all list and the Lifetime breakdown. A row [inRowGroup] is a cover row of a
 * row group card; otherwise it sits flush inside the padding of a statistics card.
 */
@Composable
internal fun StatisticsTopTitleRow(
    rank: Int?,
    title: String,
    cover: EntryCoverData,
    type: EntryType,
    typeLabel: String?,
    completionCount: Long,
    durationMillis: Long,
    shareOf: Long,
    color: Color,
    formatDuration: (Long) -> String,
    onClick: () -> Unit,
    inRowGroup: Boolean = false,
) {
    val share = if (shareOf > 0L) durationMillis.toFloat() / shareOf else 0f
    val details = listOfNotNull(
        typeLabel,
        completionCount.takeIf { it > 0L }?.let { count ->
            pluralStringResource(type.entryTypePresentation().childCountPlural, count.toInt(), count.toInt())
        },
    ).joinToString(" · ")
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
        TopTitleShare(
            durationMillis = durationMillis,
            share = share,
            shareOf = shareOf,
            formatDuration = formatDuration,
        )
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
            TopTitleLines(title = title, details = details, share = share, color = color)
        }
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
    ) {
        rankText()
        EntryCover.Book(
            data = cover,
            modifier = Modifier.width(StatisticsMetrics.rankedCoverWidth),
            frame = CoverFrame.Thumbnail,
        )
        Column(Modifier.weight(1f)) {
            TopTitleLines(title = title, details = details, share = share, color = color)
        }
        trailing()
    }
}

@Composable
private fun ColumnScope.TopTitleLines(title: String, details: String, share: Float, color: Color) {
    Text(text = title, role = TextRole.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
    if (details.isNotEmpty()) {
        Text(text = details, role = TextRole.Supporting, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    StatisticsShareBar(
        fraction = share,
        color = color,
        modifier = Modifier.padding(top = KatariTheme.spacing.s),
    )
}

// A fixed width keeps every share bar on the same scale regardless of the duration text.
@Composable
private fun TopTitleShare(durationMillis: Long, share: Float, shareOf: Long, formatDuration: (Long) -> String) {
    Column(Modifier.widthIn(min = StatisticsMetrics.rankedTrailingWidth), horizontalAlignment = Alignment.End) {
        Text(formatDuration(durationMillis), role = TextRole.Label)
        if (shareOf > 0L) {
            val percent = (share * 100).roundToInt()
            Text(
                text = if (percent == 0 && durationMillis > 0L) {
                    stringResource(MR.strings.statistics_share_below_percent, 1)
                } else {
                    stringResource(MR.strings.statistics_share_of_time, percent)
                },
                role = TextRole.Badge,
                tone = ContentTone.Secondary,
            )
        }
    }
}
