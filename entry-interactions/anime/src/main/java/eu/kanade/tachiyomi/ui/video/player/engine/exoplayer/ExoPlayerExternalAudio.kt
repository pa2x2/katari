package eu.kanade.tachiyomi.ui.video.player.engine.exoplayer

import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import eu.kanade.tachiyomi.source.entry.VideoStream
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Sets [stream] with its external audio tracks merged in after it, each a source of its own, so their tracks are the
 * `<n>:` ones the player maps back to the stream's audio tracks. ExoPlayer can't keep a separate audio file in step
 * with a live stream, so once the stream turns out to be live it plays on without them.
 */
@OptIn(markerClass = [UnstableApi::class])
internal fun ExoPlayer.setStreamWithExternalAudio(
    factory: MediaSource.Factory,
    stream: VideoStream,
    metadata: MediaMetadata,
) {
    val video = { factory.createMediaSourceOrFailing(stream.toMediaItem(metadata)) }
    if (stream.audioTracks.isEmpty()) {
        setMediaSource(video())
        return
    }
    val audio = stream.audioTracks.map { factory.createMediaSourceOrFailing(it.toMediaItem()) }
    setMediaSource(
        MergingMediaSource(
            /* adjustPeriodTimeOffsets = */
            true,
            /* clipDurations = */
            true,
            video(),
            *audio.toTypedArray(),
        ),
    )
    addListener(
        object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (timeline.isEmpty) return
                removeListener(this)
                if (!timeline.getWindow(0, Timeline.Window()).isLive) return
                logcat(LogPriority.WARN) { "Live stream '${stream.key}': its external audio tracks are left out" }
                setMediaSource(video())
                prepare()
            }
        },
    )
}
