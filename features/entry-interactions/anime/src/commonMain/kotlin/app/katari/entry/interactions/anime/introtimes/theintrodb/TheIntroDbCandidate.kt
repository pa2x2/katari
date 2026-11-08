package app.katari.entry.interactions.anime.introtimes.theintrodb

import tachiyomi.domain.entry.model.TheIntroDbMedia
import tachiyomi.domain.entry.model.TheIntroDbPlacement

/**
 * A series or film found by title that TheIntroDB can be asked about by one of its ids. [titles] are every name it is
 * known by, to match a source's title against; [title] is the one shown.
 */
internal data class TheIntroDbCandidate(
    val tvdbId: Long?,
    val tmdbId: Long?,
    val imdbId: String?,
    val title: String,
    val titles: List<String>,
    val year: Int?,
    val film: Boolean,
    val coverUrl: String?,
) {
    /** [placement] is null for a film. */
    fun toMedia(placement: TheIntroDbPlacement?) = TheIntroDbMedia(
        tvdbId = tvdbId,
        tmdbId = tmdbId,
        imdbId = imdbId,
        title = title,
        year = year,
        placement = placement,
    )

    fun isSameAs(other: TheIntroDbCandidate): Boolean =
        film == other.film && sharesId(other.tvdbId, other.tmdbId, other.imdbId)

    fun isSameAs(media: TheIntroDbMedia): Boolean =
        film == (media.placement == null) && sharesId(media.tvdbId, media.tmdbId, media.imdbId)

    // TMDB numbers series and films apart, so ids are only compared between two of the same kind.
    private fun sharesId(tvdbId: Long?, tmdbId: Long?, imdbId: String?): Boolean =
        (this.tvdbId != null && this.tvdbId == tvdbId) ||
            (this.tmdbId != null && this.tmdbId == tmdbId) ||
            (this.imdbId != null && this.imdbId == imdbId)
}
