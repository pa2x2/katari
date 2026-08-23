package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsProgress
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsTopTitle
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

internal fun StatsRange.startLocalDate(today: LocalDate): LocalDate? = when (this) {
    StatsRange.SEVEN_DAYS -> today.minusDays(6L)
    StatsRange.THIRTY_DAYS -> today.minusDays(29L)
    StatsRange.ONE_YEAR -> today.minusYears(1L).plusDays(1L)
    StatsRange.ALL -> null
}

internal fun buildLibraryProgress(items: List<LibraryItem>): StatsProgress? {
    if (items.any { !it.hasProgressSummary }) return null
    var notStarted = 0
    var inProgress = 0
    var caughtUp = 0
    var completed = 0
    items.forEach { item ->
        val consumed = checkNotNull(item.consumedCount)
        val total = checkNotNull(item.totalCount)
        when {
            consumed <= 0L -> notStarted += 1
            total > 0L && consumed >= total && item.entry.status == EntryStatus.COMPLETED -> completed += 1
            total > 0L && consumed >= total -> caughtUp += 1
            else -> inProgress += 1
        }
    }
    return StatsProgress(notStarted, inProgress, caughtUp, completed)
}

internal fun buildActivity(
    snapshot: StatisticsActivitySnapshot,
    range: StatsRange,
    types: List<EntryType>,
    today: LocalDate,
    locale: Locale,
): StatsActivity {
    val bucketUnit = range.bucketUnit(snapshot, today)
    val firstDate = range.startLocalDate(today)
        ?: snapshot.activity.minOfOrNull { LocalDate.parse(it.localDate) }
        ?: today
    val firstBucket = firstDate.bucketStart(bucketUnit, locale)
    val lastBucket = today.bucketStart(bucketUnit, locale)

    val durationByBucket = snapshot.activity.groupBy { activity ->
        LocalDate.parse(activity.localDate).bucketStart(bucketUnit, locale)
    }.mapValues { (_, rows) ->
        rows.groupBy { it.type }.mapValues { (_, typeRows) -> typeRows.sumOf { it.durationMillis } }
    }

    val trend = buildList {
        var bucket = firstBucket
        while (!bucket.isAfter(lastBucket)) {
            val values = durationByBucket[bucket].orEmpty()
            add(
                StatsTrendPoint(
                    startDate = bucket,
                    endDate = bucket.endDate(bucketUnit),
                    durationByType = types.associateWith { values[it] ?: 0L },
                ),
            )
            bucket = bucket.next(bucketUnit)
        }
    }

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
        var streakDay = today
        var streak = 0
        while (streakDay in qualifyingDays) {
            streak += 1
            streakDay = streakDay.minusDays(1L)
        }
        return streak
    }

    return StatsActivity(
        totalDurationMillis = snapshot.activity.sumOf { it.durationMillis },
        currentStreakDays = currentStreak(null),
        currentStreakDaysByType = types.associateWith(::currentStreak),
        completionCount = snapshot.completions.sumOf { it.count },
        trend = trend,
        topTitles = snapshot.topEntries.map { entry ->
            StatsTopTitle(entry.entryId, entry.type, entry.title, entry.durationMillis)
        },
        trackingStartedAtEpochMillis = snapshot.trackingStartedAtEpochMillis,
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
