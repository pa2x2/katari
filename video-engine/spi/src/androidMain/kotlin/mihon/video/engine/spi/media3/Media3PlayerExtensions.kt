package mihon.video.engine.spi.media3

import kotlinx.coroutines.flow.StateFlow
import mihon.video.engine.spi.chapters.FileChapter

/*
 * What a Media3 player can do beyond Media3's own Player, for Media3VideoPlayer to fold into the VideoPlayer contract.
 * An engine's player implements the ones it supports.
 */

/** Draws some subtitles itself, in their own styling, instead of publishing them as `Player.currentCues`. */
interface SelfDrawnSubtitles {
    /** Whether the selected subtitle track is one the player draws itself. */
    val drawsSelectedSubtitle: Boolean
}

/** Scales and moves the picture inside its video output itself; see `VideoPlayer.placePicture`. */
interface SelfPlacedPicture {
    fun placePicture(scale: Double, offsetX: Double, offsetY: Double)
}

/** Reads the chapters stored in the media file, which aren't part of Media3's state. */
interface FileChapters {
    /** Empty until the file is loaded, and for a file without any. */
    val fileChapters: StateFlow<List<FileChapter>>
}

/** Plays louder than full volume; see `VideoPlayer.setVolumeBoost`. */
interface VolumeBoost {
    /** False where the device can't make it louder, such as a phone without the audio effect it takes. */
    val canBoostVolume: Boolean

    fun setVolumeBoost(percent: Int)
}
