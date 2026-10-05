package tachiyomi.domain.statistics.entry

/**
 * Everything recorded for one title, or for all members of a merged title.
 *
 * Time recorded before detailed tracking existed only survives as per-chapter totals, so it adds to
 * [totalDurationMillis] without sessions, dates or months.
 */
data class EntryActivitySummary(
    val totalDurationMillis: Long,
    /** Part of [totalDurationMillis] recorded before detailed tracking started. */
    val untrackedDurationMillis: Long,
    /** Sessions long enough to count as activity in Statistics. */
    val sessionCount: Long,
    val sessionDurationMillis: Long,
    /** Earliest of the first session and the first chapter read; null when nothing was read. */
    val startedAtEpochMillis: Long?,
    val lastRead: EntryActivityLastRead?,
    /** Latest chapter completion, consumed or marked. */
    val lastCompletionAtEpochMillis: Long?,
    /** Local date of the first detailed activity, `yyyy-MM-dd`. */
    val firstActivityLocalDate: String?,
    val monthlyDurations: List<EntryActivityMonth>,
)

data class EntryActivityLastRead(
    val chapterName: String,
    val atEpochMillis: Long,
)

/** @param month `yyyy-MM`. */
data class EntryActivityMonth(
    val month: String,
    val durationMillis: Long,
)

/** Recorded time per chapter, from every session and from before detailed tracking. */
data class EntryChapterDuration(
    val chapterId: Long,
    val read: Boolean,
    val durationMillis: Long,
)

/** @param sampleCount how many finished chapters the median was taken from. */
data class EntryChapterPace(
    val medianMillis: Long,
    val sampleCount: Long,
)
