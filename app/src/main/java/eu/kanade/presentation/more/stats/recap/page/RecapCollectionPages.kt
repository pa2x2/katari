package eu.kanade.presentation.more.stats.recap.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.recap.components.RecapCountUp
import eu.kanade.presentation.more.stats.recap.components.RecapCover
import eu.kanade.presentation.more.stats.recap.components.RecapKicker
import eu.kanade.presentation.more.stats.recap.components.RecapText
import eu.kanade.presentation.more.stats.recap.components.RecapTypography
import eu.kanade.presentation.more.stats.recap.components.recapConsumedText
import eu.kanade.presentation.more.stats.recap.components.recapReveal
import eu.kanade.presentation.more.stats.recap.components.rememberRecapFormats
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ColumnScope.RecapFinishedPage(page: StatisticsRecapPage.Finished) {
    val context = LocalContext.current
    RecapKicker(stringResource(MR.strings.statistics_recap_finished_kicker))
    RecapCountUp(
        value = page.count.toLong(),
        format = { context.pluralStringResource(MR.plurals.statistics_title_count, it.toInt(), it.toInt()) },
        style = RecapTypography.Display,
        order = 1,
        modifier = Modifier.recapReveal(1),
    )
    page.titles.take(FINISHED_COVERS).chunked(FINISHED_COLUMNS).forEachIndexed { row, titles ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.recapReveal(row + 2)) {
            titles.forEach { RecapCover(it, width = 44.dp) }
        }
    }
    Spacer(Modifier.weight(1f))
    if (page.consumed.isNotEmpty()) {
        RecapText(
            text = stringResource(
                MR.strings.statistics_recap_along_the_way,
                page.consumed.map { recapConsumedText(it) }.joinToString(", "),
            ),
            style = RecapTypography.Body,
            order = 4,
        )
    }
}

@Composable
internal fun ColumnScope.RecapNewTitlesPage(page: StatisticsRecapPage.NewTitles) {
    val formats = rememberRecapFormats()
    val formatDuration = rememberStatisticsDurationFormatter()
    RecapKicker(stringResource(MR.strings.statistics_recap_new_kicker))
    RecapCountUp(
        value = page.count.toLong(),
        format = formats::number,
        style = RecapTypography.Huge,
        order = 1,
        modifier = Modifier.recapReveal(1),
    )
    RecapText(
        text = pluralStringResource(MR.plurals.statistics_recap_new_titles, page.count),
        style = RecapTypography.Body,
        order = 2,
    )
    Spacer(Modifier.weight(1f))
    page.standout?.let { standout ->
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.recapReveal(3),
        ) {
            RecapCover(standout, width = 112.dp)
            RecapText(
                text = stringResource(
                    MR.strings.statistics_recap_new_standout,
                    standout.title,
                    formatDuration(standout.durationMillis),
                ),
                style = RecapTypography.Body,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private const val FINISHED_COLUMNS = 6
private const val FINISHED_COVERS = 18
