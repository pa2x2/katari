package app.katari.entry.interactions.player

import tachiyomi.domain.entry.model.EntryCover

/**
 * What the desktop's media controls show of the player at one moment. [episode] tells episodes apart, a new number for
 * each one the player plays, so the controls see a change even between episodes of the same name. [durationMs] is
 * null while the length is unknown, which also leaves seeking out. [positionMs] was the position when the state was
 * made; while [playing] the controls move it on at [speed] themselves.
 */
data class AnimePlayerMediaState(
    val episode: Int,
    val title: String?,
    val series: String?,
    val artwork: AnimePlayerMediaArtwork,
    val durationMs: Long?,
    val positionMs: Long,
    val playing: Boolean,
    val speed: Float,
    val hasPreviousEpisode: Boolean,
    val hasNextEpisode: Boolean,
)

/** The episode's own still from its source ([episodePreviewUrl]) when it has one, else the title's [cover]. */
data class AnimePlayerMediaArtwork(
    val episodePreviewUrl: String?,
    val cover: EntryCover,
)
