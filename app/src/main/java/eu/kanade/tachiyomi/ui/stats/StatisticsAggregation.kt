package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsTopTitle
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

internal fun StatsRange.startLocalDate(today: LocalDate): LocalDate? = when (this) {
    StatsRange.SEVEN_DAYS -> today.minusDays(6L)
    StatsRange.THIRTY_DAYS -> today.minusDays(29L)
    StatsRange.ONE_YEAR -> today.minusYears(1L).plusDays(1L)
    StatsRange.ALL -> null
}

internal fun buildActivity(
    snapshot: StatisticsActivitySnapshot,
    range: StatsRange,
    types: List<EntryType>,
    today: LocalDate,
    locale: Locale,
): StatsActivity {
    return buildWindowActivity(
        snapshot = snapshot,
        window = range.windowEndingOn(today, isLatest = true),
        types = types,
        locale = locale,
    )
}

internal fun buildWindowActivity(
    snapshot: StatisticsActivitySnapshot,
    window: StatsActivityWindow,
    types: List<EntryType>,
    locale: Locale,
    zoneId: ZoneId = ZoneId.systemDefault(),
    navigationTimeline: StatisticsActivityTimeline = StatisticsActivityTimeline(
        activity = snapshot.activity,
        completions = snapshot.completions,
    ),
    navigationStartDate: LocalDate? = window.startDate,
    navigationEndDate: LocalDate = window.endDate,
): StatsActivity {
    val range = window.range
    val endDate = window.endDate
    val earliestDetailedDate = (snapshot.activity.map { it.localDate } + snapshot.completions.map { it.localDate })
        .minOrNull()
        ?.let(LocalDate::parse)
    val trackingStartDate = snapshot.trackingStartedAtEpochMillis
        ?.let { startedAt -> Instant.ofEpochMilli(startedAt).atZone(zoneId).toLocalDate() }
        ?: earliestDetailedDate
    val bucketUnit = range.bucketUnit(snapshot, endDate)
    val firstDate = window.startDate
        ?: trackingStartDate
        ?: snapshot.activity.minOfOrNull { LocalDate.parse(it.localDate) }
        ?: endDate
    val durationByBucket = navigationTimeline.activity.groupBy { activity ->
        LocalDate.parse(activity.localDate).bucketStart(bucketUnit, locale)
    }.mapValues { (_, rows) ->
        rows.groupBy { it.type }.mapValues { (_, typeRows) -> typeRows.sumOf { it.durationMillis } }
    }
    val completionsByBucket = navigationTimeline.completions.groupBy { completion ->
        LocalDate.parse(completion.localDate).bucketStart(bucketUnit, locale)
    }.mapValues { (_, rows) ->
        rows.groupBy { it.type }.mapValues { (_, typeRows) -> typeRows.sumOf { it.count } }
    }

    fun buildTrend(trendStartDate: LocalDate, trendEndDate: LocalDate): List<StatsTrendPoint> {
        val trendFirstBucket = trendStartDate.bucketStart(bucketUnit, locale)
        val trendLastBucket = trendEndDate.bucketStart(bucketUnit, locale)
        return buildList {
            var bucket = trendFirstBucket
            while (!bucket.isAfter(trendLastBucket)) {
                val values = durationByBucket[bucket].orEmpty()
                val completions = completionsByBucket[bucket].orEmpty()
                val pointStart = maxOf(bucket, trendStartDate)
                val pointEnd = minOf(bucket.endDate(bucketUnit), trendEndDate)
                add(
                    StatsTrendPoint(
                        bucketStartDate = bucket,
                        startDate = pointStart,
                        endDate = pointEnd,
                        durationByType = types.associateWith { values[it] ?: 0L },
                        completionCountByType = types.associateWith { completions[it] ?: 0L },
                        trackedStartDate = trackingStartDate
                            ?.takeUnless(pointEnd::isBefore)
                            ?.let { maxOf(pointStart, it) },
                    ),
                )
                bucket = bucket.next(bucketUnit)
            }
        }
    }
    val trend = buildTrend(firstDate, endDate)
    val navigationTrend = buildTrend(navigationStartDate ?: firstDate, navigationEndDate)

    fun currentStreak(type: EntryType?): Int {
        val qualifyingDays = buildSet {
            snapshot.activity
                .filter { type == null || it.type == type }
                .groupBy { it.localDate }
                .filterValues { rows -> rows.sumOf { it.durationMillis } >= STREAK_DURATION_MILLIS }
                .keys
                .mapTo(this) { LocalDate.parse(it) }
            snapshot.completions
                .filter { it.count > 0L && (type == null || it.type == type) }
                .mapTo(this) { LocalDate.parse(it.localDate) }
        }
        var streakDay = endDate
        var streak = 0
        while (streakDay in qualifyingDays) {
            streak += 1
            streakDay = streakDay.minusDays(1L)
        }
        return streak
    }

    return StatsActivity(
        window = window.copy(startDate = firstDate),
        totalDurationMillis = snapshot.activity.sumOf { it.durationMillis },
        currentStreakDays = currentStreak(null),
        currentStreakDaysByType = types.associateWith(::currentStreak),
        completionCount = snapshot.completions.sumOf { it.count },
        completionCountByType = types.associateWith { type ->
            snapshot.completions.filter { it.type == type }.sumOf { it.count }
        },
        sessionCount = snapshot.sessions.sumOf { it.sessionCount },
        sessionCountByType = types.associateWith { type ->
            snapshot.sessions.firstOrNull { it.type == type }?.sessionCount ?: 0L
        },
        averageSessionDurationMillis = snapshot.activity.sumOf { it.durationMillis }
            .div(snapshot.sessions.sumOf { it.sessionCount }.coerceAtLeast(1L)),
        averageSessionDurationByType = types.associateWith { type ->
            snapshot.sessions.firstOrNull { it.type == type }?.averageDurationMillis ?: 0L
        },
        longestSessionDurationMillis = snapshot.sessions.maxOfOrNull { it.longestDurationMillis } ?: 0L,
        longestSessionDurationByType = types.associateWith { type ->
            snapshot.sessions.firstOrNull { it.type == type }?.longestDurationMillis ?: 0L
        },
        activeDays = snapshot.activity.filter { it.durationMillis > 0L }.distinctBy { it.localDate }.size,
        activeDaysByType = types.associateWith { type ->
            snapshot.activity.filter { it.type == type && it.durationMillis > 0L }.distinctBy { it.localDate }.size
        },
        trend = trend,
        navigationTrend = navigationTrend,
        topTitles = snapshot.topEntries.map { entry ->
            StatsTopTitle(entry.entryId, entry.type, entry.title, entry.durationMillis)
        },
        trackingStartedAtEpochMillis = snapshot.trackingStartedAtEpochMillis,
        trackingStartDate = trackingStartDate,
        earlierDurationMillis = snapshot.earlierActivity.sumOf { it.durationMillis },
        earlierDurationByType = snapshot.earlierActivity.associate { it.type to it.durationMillis },
    )
}

private fun StatsRange.bucketUnit(snapshot: StatisticsActivitySnapshot, today: LocalDate): BucketUnit = when (this) {
    StatsRange.SEVEN_DAYS, StatsRange.THIRTY_DAYS -> BucketUnit.DAY
    StatsRange.ONE_YEAR -> BucketUnit.WEEK
    StatsRange.ALL -> {
        val earliest = snapshot.activity.minOfOrNull { LocalDate.parse(it.localDate) } ?: today
        if (earliest.isBefore(today.minusYears(2L))) BucketUnit.YEAR else BucketUnit.MONTH
    }
}

private fun LocalDate.bucketStart(unit: BucketUnit, locale: Locale): LocalDate = when (unit) {
    BucketUnit.DAY -> this
    BucketUnit.WEEK -> with(TemporalAdjusters.previousOrSame(WeekFields.of(locale).firstDayOfWeek))
    BucketUnit.MONTH -> withDayOfMonth(1)
    BucketUnit.YEAR -> withDayOfYear(1)
}

private fun LocalDate.endDate(unit: BucketUnit): LocalDate = when (unit) {
    BucketUnit.DAY -> this
    BucketUnit.WEEK -> plusDays(6L)
    BucketUnit.MONTH -> with(TemporalAdjusters.lastDayOfMonth())
    BucketUnit.YEAR -> with(TemporalAdjusters.lastDayOfYear())
}

private fun LocalDate.next(unit: BucketUnit): LocalDate = when (unit) {
    BucketUnit.DAY -> plusDays(1L)
    BucketUnit.WEEK -> plusWeeks(1L)
    BucketUnit.MONTH -> plusMonths(1L)
    BucketUnit.YEAR -> plusYears(1L)
}

private enum class BucketUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR,
}

private const val STREAK_DURATION_MILLIS = 60_000L
