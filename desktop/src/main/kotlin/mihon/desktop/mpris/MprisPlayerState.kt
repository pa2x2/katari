package mihon.desktop.mpris

/**
 * What the media player shows at one moment, in MPRIS's own terms: times in microseconds, [trackId] an object path
 * that changes with each thing played. [lengthUs] is null while the length is unknown, which also leaves seeking out.
 * [positionUs] was the position when the state was made; while [playing], the player moves it on at [rate] itself.
 */
internal data class MprisPlayerState(
    val trackId: String,
    val title: String?,
    val series: String?,
    val artUrl: String?,
    val lengthUs: Long?,
    val positionUs: Long,
    val playing: Boolean,
    val rate: Double,
    val canGoPrevious: Boolean,
    val canGoNext: Boolean,
)

/** What shells ask of the player, called on the player's serving thread. */
internal interface MprisCommands {
    fun play()

    fun pause()

    fun playPause()

    fun stop()

    fun setPosition(positionUs: Long)

    fun previous()

    fun next()

    fun raise()
}
