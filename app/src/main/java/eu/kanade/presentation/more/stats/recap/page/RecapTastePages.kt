package eu.kanade.presentation.more.stats.recap.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.recap.components.LocalRecapPalette
import eu.kanade.presentation.more.stats.recap.components.RecapKicker
import eu.kanade.presentation.more.stats.recap.components.RecapText
import eu.kanade.presentation.more.stats.recap.components.RecapTypography
import eu.kanade.presentation.more.stats.recap.components.recapTypeName
import eu.kanade.presentation.more.stats.recap.components.rememberRecapFormats
import eu.kanade.presentation.more.stats.recap.motion.RECAP_GROW_MILLIS
import eu.kanade.presentation.more.stats.recap.motion.recapReveal
import eu.kanade.presentation.more.stats.recap.motion.rememberRecapEntrance
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** How the time divided between entry types, as one split bar. */
@Composable
internal fun ColumnScope.RecapTypesPage(page: StatisticsRecapPage.Types) {
    val formats = rememberRecapFormats()
    val formatDuration = rememberStatisticsDurationFormatter()
    val palette = LocalRecapPalette.current
    val colors = listOf(palette.accent, palette.ink.copy(alpha = 0.75f), palette.muted.copy(alpha = 0.5f))
    val top = page.shares.first()
    RecapKicker(stringResource(MR.strings.statistics_recap_types_kicker))
    Spacer(Modifier.weight(1f))
    RecapText(
        text = stringResource(MR.strings.statistics_recap_type_percent, top.percent, recapTypeName(top.type)),
        style = RecapTypography.Display,
        order = 1,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(11.dp)).recapReveal(2),
    ) {
        page.shares.forEachIndexed { index, share ->
            Box(
                Modifier
                    .weight(share.durationMillis.toFloat().coerceAtLeast(1f))
                    .height(22.dp)
                    .background(colors[index % colors.size]),
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.recapReveal(3)) {
        page.shares.forEachIndexed { index, share ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(10.dp).background(colors[index % colors.size], RoundedCornerShape(3.dp)))
                RecapText(
                    text = "${recapTypeName(share.type)} · ${formatDuration(share.durationMillis)}",
                    style = RecapTypography.Small,
                    color = palette.muted,
                )
            }
        }
    }
    Spacer(Modifier.weight(1f))
    page.ledMonths?.let { (type, months) ->
        RecapText(
            text = stringResource(
                MR.strings.statistics_recap_types_led,
                recapTypeName(type),
                months.joinToString(", ") { formats.month(it) },
            ),
            style = RecapTypography.Body,
            order = 4,
        )
    }
}

@Composable
internal fun ColumnScope.RecapGenresPage(page: StatisticsRecapPage.Genres) {
    val palette = LocalRecapPalette.current
    val topPercent = page.genres.first().percent.coerceAtLeast(1)
    RecapKicker(stringResource(MR.strings.statistics_recap_genres_kicker))
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 12.dp)) {
        page.genres.forEachIndexed { index, genre ->
            val grow by rememberRecapEntrance(index + 1, RECAP_GROW_MILLIS)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.recapReveal(index + 1)) {
                RecapText(
                    text = genre.name,
                    style = if (index == 0) RecapTypography.Headline else RecapTypography.Title,
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .fillMaxWidth(genre.percent.toFloat() / topPercent * grow.coerceAtLeast(0.01f))
                        .height(6.dp)
                        .background(if (index == 0) palette.accent else palette.accent.copy(alpha = 0.5f), BarShape),
                )
            }
        }
    }
    Spacer(Modifier.weight(1f))
    RecapText(
        text = stringResource(
            MR.strings.statistics_recap_genre_titles,
            page.genres.first().name,
            page.topGenreTitles,
            page.topTitleCount,
        ),
        style = RecapTypography.Body,
        order = 5,
    )
}

private val BarShape = RoundedCornerShape(3.dp)
