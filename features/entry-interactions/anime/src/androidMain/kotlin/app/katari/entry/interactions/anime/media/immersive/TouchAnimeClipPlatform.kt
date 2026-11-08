package app.katari.entry.interactions.anime.media.immersive

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import app.katari.video.engine.spi.player.VideoPlayer
import eu.kanade.tachiyomi.ui.video.player.gesture.resolveVideoPlayerSeekDirectionFromTap
import eu.kanade.tachiyomi.ui.video.player.surface.VideoSurface
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.entry.model.PictureFraming

/**
 * Android's feed clips: a tap shows or hides the controls, a double tap on either side seeks, and holding plays at 2×.
 * The window keeps the screen on while a clip plays, and raising the media volume unmutes the feed.
 */
internal object TouchAnimeClipPlatform : AnimeClipPlatform {
    @Composable
    override fun Picture(player: VideoPlayer, aspectRatio: Float?, target: AnimeClipInputTarget, modifier: Modifier) {
        Box(modifier = modifier) {
            VideoSurface(
                player = player,
                framing = { PictureFraming.Fit },
                aspectRatio = { aspectRatio },
                modifier = Modifier.fillMaxSize(),
            )
            Box(modifier = Modifier.fillMaxSize().clipTouchGestures(target))
        }
    }

    @Composable
    override fun KeepAwake(playing: Boolean) {
        if (playing) Box(modifier = Modifier.keepScreenOn())
    }

    @Composable
    override fun UnmuteOnVolumeRaise(active: Boolean, muted: Boolean, onUnmute: () -> Unit) {
        val context = LocalContext.current
        val latestMuted by rememberUpdatedState(muted)
        val latestOnUnmute by rememberUpdatedState(onUnmute)
        val audioManager = remember(context) {
            context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        DisposableEffect(active, audioManager) {
            if (!active) return@DisposableEffect onDispose {}

            var previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    if (latestMuted && currentVolume > previousVolume) latestOnUnmute()
                    previousVolume = currentVolume
                }
            }
            context.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                volumeObserver,
            )
            onDispose { context.contentResolver.unregisterContentObserver(volumeObserver) }
        }
    }
}

private fun Modifier.clipTouchGestures(target: AnimeClipInputTarget): Modifier = pointerInput(target) {
    var suppressNextTap = false
    detectTapGestures(
        onDoubleTap = { offset ->
            suppressNextTap = false
            resolveVideoPlayerSeekDirectionFromTap(
                tapX = offset.x,
                viewportWidth = size.width.toFloat(),
            )?.let { target.seek(it, DOUBLE_TAP_SEEK_SECONDS) }
        },
        onTap = { offset ->
            if (suppressNextTap) {
                suppressNextTap = false
            } else if (target.seekFeedbackShown) {
                resolveVideoPlayerSeekDirectionFromTap(
                    tapX = offset.x,
                    viewportWidth = size.width.toFloat(),
                )?.let { target.seek(it, DOUBLE_TAP_SEEK_SECONDS) }
            } else {
                target.toggleControls()
            }
        },
        onPress = {
            coroutineScope {
                var boosted = false
                val boostJob = launch {
                    delay(SPEED_BOOST_PRESS_DELAY_MS)
                    target.setSpeedBoost(true)
                    boosted = true
                }
                var released = false
                try {
                    released = tryAwaitRelease()
                } finally {
                    boostJob.cancel()
                    if (boosted) {
                        suppressNextTap = released
                        target.setSpeedBoost(false)
                    }
                }
            }
        },
    )
}

private const val SPEED_BOOST_PRESS_DELAY_MS = 350L

// Short clips in a feed; the anime player's seek step setting is for full episodes.
private const val DOUBLE_TAP_SEEK_SECONDS = 5
