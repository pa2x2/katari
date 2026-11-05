package mihon.entry.interactions.anime.media.immersive

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.core.common.platform.PlatformFacility
import mihon.video.engine.spi.player.VideoPlayer

/**
 * What a clip in the immersive feed takes from the platform it plays on. Android's are touch gestures, the screen kept
 * on by the window and unmuting when the media volume is raised; desktop's are the mouse and keys, and the screen kept
 * awake through the session while the window is seen.
 */
@PlatformFacility
internal interface AnimeClipPlatform {
    /** [player]'s picture, fitted, taking the viewer's input for [target]. */
    @Composable
    fun Picture(player: VideoPlayer, aspectRatio: Float?, target: AnimeClipInputTarget, modifier: Modifier)

    /** Keeps the screen awake while [playing], where the clip is seen. */
    @Composable
    fun KeepAwake(playing: Boolean)

    /** Runs [onUnmute] when the viewer raises the device's volume while the [active] clip is [muted]. */
    @Composable
    fun UnmuteOnVolumeRaise(active: Boolean, muted: Boolean, onUnmute: () -> Unit)
}
