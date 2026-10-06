package eu.kanade.presentation.more.stats.recap.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.recap.components.RecapCover
import eu.kanade.presentation.more.stats.recap.components.RecapFormats
import eu.kanade.presentation.more.stats.recap.components.RecapKicker
import eu.kanade.presentation.more.stats.recap.components.RecapText
import eu.kanade.presentation.more.stats.recap.components.RecapTypography
import eu.kanade.presentation.more.stats.recap.components.recapItemsText
import eu.kanade.presentation.more.stats.recap.components.rememberRecapFormats
import eu.kanade.presentation.more.stats.recap.motion.RecapCountUp
import eu.kanade.presentation.more.stats.recap.motion.recapFloat
import eu.kanade.presentation.more.stats.recap.motion.recapReveal
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The first or last title of the period, at the moment it was opened. */
@Composable
internal fun ColumnScope.RecapBookendPage(page: StatisticsRecapPage.Bookend) {
    val formats = rememberRecapFormats()
    val time = formats.time(page.at.toLocalTime())
    RecapKicker(formats.weekdayDayMonth(page.at))
    Spacer(Modifier.weight(0.4f))
    RecapCover(page.title, width = 176.dp, modifier = Modifier.recapReveal(1).recapFloat())
    Spacer(Modifier.weight(1f))
    RecapText(
        text = stringResource(
            if (page.isFirst) MR.strings.statistics_recap_first_title else MR.strings.statistics_recap_last_title,
            page.title.title,
        ),
        style = RecapTypography.Headline,
        maxLines = 3,
        order = 2,
    )
    RecapText(
        text = page.item?.let { stringResource(MR.strings.statistics_recap_item_at, recapItemsText(it), time) }
            ?: stringResource(MR.strings.statistics_recap_at, time),
        style = RecapTypography.Body,
        order = 3,
    )
}

@Composable
internal fun ColumnScope.RecapBiggestDayPage(page: StatisticsRecapPage.BiggestDay) {
    val formats = rememberRecapFormats()
    val formatDuration = rememberStatisticsDurationFormatter()
    RecapKicker(stringResource(MR.strings.statistics_recap_biggest_day_kicker))
    RecapText(formats.weekdayDayMonth(page.date), RecapTypography.Headline, order = 1)
    Spacer(Modifier.weight(1f))
    RecapCountUp(
        value = page.durationMillis,
        format = formatDuration,
        style = RecapTypography.Display,
        order = 2,
        modifier = Modifier.recapReveal(2),
    )
    Spacer(Modifier.weight(1f))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.recapReveal(3),
    ) {
        page.title?.let { RecapCover(it, width = 72.dp, modifier = Modifier.recapFloat()) }
        RecapText(biggestDayText(page, formats), RecapTypography.Body, modifier = Modifier.weight(1f))
    }
}

/** What a biggest day held and when it ran, as one sentence. */
@Composable
internal fun biggestDayText(page: StatisticsRecapPage.BiggestDay, formats: RecapFormats): String {
    val from = formats.time(page.from)
    val to = formats.time(page.to)
    val title = page.title ?: return stringResource(MR.strings.statistics_recap_day_hours, from, to)
    return when {
        !page.isMostlyOneTitle -> stringResource(MR.strings.statistics_recap_day_mostly, title.title, from, to)
        page.items != null -> stringResource(
            MR.strings.statistics_recap_day_items,
            title.title,
            recapItemsText(page.items),
            from,
            to,
        )
        else -> stringResource(MR.strings.statistics_recap_day_title, title.title, from, to)
    }
}
