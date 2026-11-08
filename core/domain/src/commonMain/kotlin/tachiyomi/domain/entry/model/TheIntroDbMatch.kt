package tachiyomi.domain.entry.model

/**
 * The series or film whose TheIntroDB intro and credits times an entry's episodes use. Like [IntroTimesMatch] it is
 * kept per season group ([groupKey]), but each service finds its own, so neither depends on the other.
 *
 * @property media null when none is used: none was found by title, or the viewer picked None.
 * @property pickedByUser picked in the player rather than found by title, so it is never searched again by itself.
 * @property checkedAt when it was found or picked, in epoch milliseconds.
 */
data class TheIntroDbMatch(
    val entryId: Long,
    val groupKey: String?,
    val media: TheIntroDbMedia?,
    val pickedByUser: Boolean,
    val checkedAt: Long,
)

/**
 * A series or film TheIntroDB can be asked about by any one of its ids, at least one of which is set. [tmdbId] is
 * TMDB's TV id for a series and its movie id for a film, as TheIntroDB tells them apart by whether an episode is asked.
 *
 * @property placement where the matched episodes sit in the series; null for a film.
 */
data class TheIntroDbMedia(
    val tvdbId: Long?,
    val tmdbId: Long?,
    val imdbId: String?,
    val title: String,
    val year: Int?,
    val placement: TheIntroDbPlacement?,
)

/**
 * The season a scope's episodes are in and the number its first episode has there, so that the scope's n-th episode is
 * [firstEpisode] + n - 1 even when its source numbers the episodes from 1 within a season split in parts.
 */
data class TheIntroDbPlacement(
    val season: Int,
    val firstEpisode: Int,
)
