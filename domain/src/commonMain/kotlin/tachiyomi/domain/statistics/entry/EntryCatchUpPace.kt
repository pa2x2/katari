package tachiyomi.domain.statistics.entry

/**
 * How long an unfinished chapter of a title is expected to take: the title's own median once it has enough finished,
 * timed chapters, otherwise the median for its type when chapters of that type take comparable time.
 */
object EntryCatchUpPace {
    /** Fewer finished chapters say more about one sitting than about the title. */
    const val MINIMUM_SAMPLES = 3L

    fun titlePace(durations: List<EntryChapterDuration>): EntryChapterPace? {
        val finished = durations.filter(EntryChapterDuration::read).map(EntryChapterDuration::durationMillis).sorted()
        if (finished.isEmpty()) return null
        return EntryChapterPace(medianMillis = finished[(finished.size - 1) / 2], sampleCount = finished.size.toLong())
    }

    /** @param borrowTypePace whether chapters of different titles of this type take comparable time. */
    fun select(title: EntryChapterPace?, type: EntryChapterPace?, borrowTypePace: Boolean): Long? {
        title?.takeIf { it.sampleCount >= MINIMUM_SAMPLES }?.let { return it.medianMillis }
        if (!borrowTypePace) return null
        return type?.takeIf { it.sampleCount >= MINIMUM_SAMPLES }?.medianMillis
    }
}
