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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.recap.components.LocalRecapPalette
import eu.kanade.presentation.more.stats.recap.components.RecapKicker
import eu.kanade.presentation.more.stats.recap.components.RecapText
import eu.kanade.presentation.more.stats.recap.components.recapTypeName
import eu.kanade.presentation.more.stats.recap.components.rememberRecapFormats
import eu.kanade.presentation.more.stats.recap.motion.RECAP_GROW_MILLIS
import eu.kanade.presentation.more.stats.recap.motion.recapReveal
import eu.kanade.presentation.more.stats.recap.motion.rememberRecapEntrance
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import mihon.presentation.kit.content.LegendSwatch
import mihon.presentation.kit.content.StoryMetrics
import mihon.presentation.kit.content.chartBar
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** How the time divided between entry types, as one split bar. */
@Composable
internal fun ColumnScope.RecapTypesPage(page: StatisticsRecapPage.Types) {
    val formats = rememberRecapFormats()
    val formatDuration = rememberStatisticsDurationFormatter()
    val palette = LocalRecapPalette.current
    val colors = palette.series
    val top = page.shares.first()
    RecapKicker(stringResource(MR.strings.statistics_recap_types_kicker))
    Spacer(Modifier.weight(1f))
    RecapText(
        text = stringResource(MR.strings.statistics_recap_type_percent, top.percent, recapTypeName(top.type)),
        role = TextRole.Display,
        order = 1,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
        modifier = Modifier
            .fillMaxWidth()
            .height(StoryMetrics.splitBarHeight)
            .chartBar()
            .recapReveal(2),
    ) {
        page.shares.forEachIndexed { index, share ->
            Box(
                Modifier
                    .weight(share.durationMillis.toFloat().coerceAtLeast(1f))
                    .height(StoryMetrics.splitBarHeight)
                    .background(colors[index % colors.size]),
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s), modifier = Modifier.recapReveal(3)) {
        page.shares.forEachIndexed { index, share ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                LegendSwatch(colors[index % colors.size])
                RecapText(
                    text = "${recapTypeName(share.type)} · ${formatDuration(share.durationMillis)}",
                    role = TextRole.Supporting,
                    tone = ContentTone.Secondary,
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
            role = TextRole.Body,
            order = 4,
        )
    }
}

@Composable
internal fun ColumnScope.RecapGenresPage(page: StatisticsRecapPage.Genres) {
    val palette = LocalRecapPalette.current
    val topPercent = page.genres.first().percent.coerceAtLeast(1)
    RecapKicker(stringResource(MR.strings.statistics_recap_genres_kicker))
    Column(
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        modifier = Modifier.padding(top = KatariTheme.spacing.m),
    ) {
        page.genres.forEachIndexed { index, genre ->
            val grow by rememberRecapEntrance(index + 1, RECAP_GROW_MILLIS)
            Column(
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                modifier = Modifier.recapReveal(index + 1),
            ) {
                RecapText(
                    text = genre.name,
                    role = if (index == 0) TextRole.Metric else TextRole.SheetTitle,
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .fillMaxWidth(genre.percent.toFloat() / topPercent * grow.coerceAtLeast(0.01f))
                        .height(StoryMetrics.rankBarHeight)
                        .chartBar()
                        .background(if (index == 0) palette.accent else palette.accentMuted),
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
        role = TextRole.Body,
        order = 5,
    )
}
