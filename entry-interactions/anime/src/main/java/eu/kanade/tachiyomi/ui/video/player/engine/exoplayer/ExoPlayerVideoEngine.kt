package eu.kanade.tachiyomi.ui.video.player.engine.exoplayer

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.VideoStream
import mihon.video.engine.spi.VideoEngine
import mihon.video.engine.spi.VideoPlayerRole

private const val SEEK_PREVIEW_MAX_WIDTH = 320
private const val SEEK_PREVIEW_MAX_HEIGHT = 180

/** The Media3 ExoPlayer engine. Every role loads through [buildExoPlayerMediaSourceFactory] and shares [mediaCache]. */
internal class ExoPlayerVideoEngine(
    private val networkHelper: NetworkHelper,
    private val mediaCache: ExoPlayerMediaCache,
) : VideoEngine {

    @OptIn(markerClass = [UnstableApi::class])
    override fun createPlayer(
        context: Context,
        stream: VideoStream,
        role: VideoPlayerRole,
    ): Player {
        val builder = ExoPlayer.Builder(
            context,
            buildExoPlayerMediaSourceFactory(context, networkHelper, mediaCache, stream),
        )
        if (role is VideoPlayerRole.Watched) {
            builder
                // The second argument hands audio focus to the player, so calls and other apps' audio pause it.
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true,
                )
                .setHandleAudioBecomingNoisy(true)
                // Keeps the CPU and Wi-Fi up while it plays with the screen off.
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setSeekBackIncrementMs(role.seekStepMs)
                .setSeekForwardIncrementMs(role.seekStepMs)
        }
        val metadata = (role as? VideoPlayerRole.Watched)?.metadata ?: MediaMetadata.EMPTY
        return builder.build().apply {
            setMediaItem(stream.toMediaItem(metadata))
            if (role == VideoPlayerRole.SeekPreview) {
                volume = 0f
                playWhenReady = false
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .setMaxVideoSize(SEEK_PREVIEW_MAX_WIDTH, SEEK_PREVIEW_MAX_HEIGHT)
                    .build()
            }
        }
    }
}
