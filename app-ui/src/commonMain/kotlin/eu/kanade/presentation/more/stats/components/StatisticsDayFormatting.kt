package eu.kanade.presentation.more.stats.components

import app.katari.core.platform.PlatformContext
import java.time.LocalDate

/*
 * Days with abbreviated month names. Android's DateUtils leaves out what a range's ends share ("Sep 15 – 21"), which
 * java.time can't, so each platform formats them through its ICU.
 */

/** A day such as "Sep 28", with its year only when [withYear]. */
internal expect fun PlatformContext.formatStatisticsDay(date: LocalDate, withYear: Boolean): String

/** The days from [start] to [endInclusive], such as "Sep 15 – 21", with the year only when [withYear]. */
internal expect fun PlatformContext.formatStatisticsDays(
    start: LocalDate,
    endInclusive: LocalDate,
    withYear: Boolean,
): String
