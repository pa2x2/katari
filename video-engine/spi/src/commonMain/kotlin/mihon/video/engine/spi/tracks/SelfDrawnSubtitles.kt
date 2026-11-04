package mihon.video.engine.spi.tracks

/**
 * A player that draws some subtitles itself, in their own styling, instead of publishing them as
 * `Player.currentCues`. The viewer's subtitle appearance doesn't reach those.
 */
interface SelfDrawnSubtitles {
    /** Whether the selected subtitle track is one the player draws itself. */
    val drawsSelectedSubtitle: Boolean
}
