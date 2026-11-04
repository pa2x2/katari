package mihon.video.engine.spi.audio

/**
 * A player that can play louder than full volume, for sources mixed too quietly. The boost is on top of the player's
 * own volume and the device's, as a level from 100 (as recorded) to [MAX_VOLUME_BOOST_PERCENT], on mpv's volume scale:
 * its software mixer applies the cube of the level, so 150 is about 10 dB louder.
 */
interface VolumeBoost {
    /** False where the device can't make it louder, such as a phone without the audio effect it takes. */
    val canBoostVolume: Boolean

    fun setVolumeBoost(percent: Int)

    companion object {
        const val NO_VOLUME_BOOST_PERCENT = 100
        const val MAX_VOLUME_BOOST_PERCENT = 150
    }
}
