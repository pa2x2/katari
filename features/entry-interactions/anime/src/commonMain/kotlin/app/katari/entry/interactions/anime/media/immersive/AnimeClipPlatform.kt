package app.katari.entry.interactions.anime.media.immersive

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.core.common.platform.PlatformFacility
import app.katari.video.engine.spi.player.VideoPlayer
import tachiyomi.core.common.preference.Preference

/**
 * What a clip in the immersive feed takes from the platform it plays on. Android's are touch gestures, the screen kept
 * on by the window and unmuting when the media volume is raised; desktop's are the mouse and keys, and the screen kept
 * awake through the session while the window is seen.
 */
@PlatformFacility
internal interface AnimeClipPlatform {
    /**
     * Whether a playing clip's controls hide by themselves a while after they show, as on a touch screen. With a mouse
     * the feed hides them once the mouse rests, so the clip leaves them be.
     */
    val controlsHideWhilePlaying: Boolean

    /**
     * The player's own volume in percent, up to [VideoPlayer.MAX_VOLUME_BOOST_PERCENT], which clips play at while not
     * muted and which the feed lets the viewer change; null where the device's volume rules instead, as on Android.
     */
    val playerVolume: Preference<Int>?

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
