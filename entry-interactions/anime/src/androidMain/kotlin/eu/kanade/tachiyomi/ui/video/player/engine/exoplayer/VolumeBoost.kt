package eu.kanade.tachiyomi.ui.video.player.engine.exoplayer

/** A Media3 player that plays louder than full volume, for [Media3VideoPlayer]; see `VideoPlayer.setVolumeBoost`. */
internal interface VolumeBoost {
    /** False where the device can't make it louder, such as a phone without the audio effect it takes. */
    val canBoostVolume: Boolean

    fun setVolumeBoost(percent: Int)
}
