package tachiyomi.domain.entry.model

/**
 * The series whose AniSkip intro and credits times an entry's episodes use. An entry with several season groups has
 * one match per group ([groupKey]); otherwise [groupKey] is null and the match covers the whole entry.
 *
 * @property series null when no series is used: none was found by title, or the viewer picked None.
 * @property pickedByUser picked in the player rather than found by title, so it is never searched again by itself.
 * @property checkedAt when the series was found or picked, in epoch milliseconds.
 */
data class IntroTimesMatch(
    val entryId: Long,
    val groupKey: String?,
    val series: IntroTimesSeries?,
    val pickedByUser: Boolean,
    val checkedAt: Long,
)

/** An AniList anime; [malId] is the MyAnimeList id AniSkip keys its times by. */
data class IntroTimesSeries(
    val anilistId: Long,
    val malId: Long,
    val title: String,
    val year: Int?,
)
