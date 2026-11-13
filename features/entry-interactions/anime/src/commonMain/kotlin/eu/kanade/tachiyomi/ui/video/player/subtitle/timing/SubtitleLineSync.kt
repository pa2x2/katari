package eu.kanade.tachiyomi.ui.video.player.subtitle.timing

/**
 * Sync to a line (D006): the viewer marks when a subtitle shows and when its line is heard, in either order, and the
 * timing moves by the time between, so the subtitle shows when its line is heard. Both are playback positions, with
 * the subtitle shown as the timing set so far has it, so the move adds to that timing.
 */
internal data class SubtitleLineSync(
    val shownAtMs: Long? = null,
    val heardAtMs: Long? = null,
) {
    fun shown(positionMs: Long) = copy(shownAtMs = positionMs)

    fun heard(positionMs: Long) = copy(heardAtMs = positionMs)

    /** How much later the subtitle should show, once both are marked; negative for earlier. */
    val shiftMs: Long?
        get() {
            val shown = shownAtMs ?: return null
            val heard = heardAtMs ?: return null
            return heard - shown
        }
}
