package tachiyomi.domain.statistics.entry

/**
 * How long a title's unfinished chapters are expected to take, from the title's own history alone: other titles, even
 * of the same type, say nothing about how long this one's chapters run.
 *
 * A chapter whose runtime is known takes what's left of it. Any other takes [typicalChapterMillis]: the median runtime
 * of the title's chapters when any is known, otherwise the median time spent on its finished chapters once there are
 * enough of them.
 */
data class EntryCatchUpEstimate(
    val remainingMillisByChapterId: Map<Long, Long>,
    /** Null when the title has nothing to go on. */
    val typicalChapterMillis: Long?,
) {
    /** Null when some of [unreadChapterIds] can't be estimated. */
    fun catchUpMillis(unreadChapterIds: Collection<Long>): Long? {
        var total = 0L
        for (chapterId in unreadChapterIds) {
            total += remainingMillisByChapterId[chapterId] ?: typicalChapterMillis ?: return null
        }
        return total
    }

    companion object {
        /** Fewer finished chapters say more about one sitting than about the title. */
        const val MINIMUM_TIMED_CHAPTERS = 3

        val Unknown = EntryCatchUpEstimate(remainingMillisByChapterId = emptyMap(), typicalChapterMillis = null)

        /** @param runtimes at most one per chapter. */
        fun of(runtimes: List<EntryChapterRuntime>, durations: List<EntryChapterDuration>): EntryCatchUpEstimate {
            val spentPerChapter = durations.filter(EntryChapterDuration::read).map(EntryChapterDuration::durationMillis)
            return EntryCatchUpEstimate(
                remainingMillisByChapterId = runtimes.associate {
                    it.chapterId to (it.lengthMillis - it.playedMillis).coerceAtLeast(0L)
                },
                typicalChapterMillis = runtimes.map(EntryChapterRuntime::lengthMillis).lowerMedian()
                    ?: spentPerChapter.takeIf { it.size >= MINIMUM_TIMED_CHAPTERS }?.lowerMedian(),
            )
        }

        private fun List<Long>.lowerMedian(): Long? = sorted().getOrNull((size - 1) / 2)
    }
}

/** How long a chapter runs and how far into it playback got, as its saved progress records them. */
data class EntryChapterRuntime(
    val chapterId: Long,
    val lengthMillis: Long,
    val playedMillis: Long,
)
