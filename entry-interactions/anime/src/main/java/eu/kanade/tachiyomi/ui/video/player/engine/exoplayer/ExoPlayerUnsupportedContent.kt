package eu.kanade.tachiyomi.ui.video.player.engine.exoplayer

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.ParserException
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.source.BaseMediaSource
import androidx.media3.exoplayer.source.MediaPeriod
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.Allocator

/**
 * The source for [mediaItem], or one that fails on prepare when this factory has no module for its content type
 * (SmoothStreaming, RTSP). The factory itself would throw while the player is made, which no caller can recover from;
 * the failing source makes the player report [androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED]
 * instead, so the watched player moves to mpv under Auto, the seek preview hides and the feed shows its error.
 */
@OptIn(markerClass = [UnstableApi::class])
internal fun MediaSource.Factory.createMediaSourceOrFailing(mediaItem: MediaItem): MediaSource {
    val configuration = mediaItem.localConfiguration ?: return createMediaSource(mediaItem)
    val contentType = Util.inferContentTypeForUriAndMimeType(configuration.uri, configuration.mimeType)
    if (contentType in supportedTypes) return createMediaSource(mediaItem)
    return UnsupportedContentMediaSource(mediaItem, contentType)
}

@OptIn(markerClass = [UnstableApi::class])
private class UnsupportedContentMediaSource(
    private val mediaItem: MediaItem,
    private val contentType: Int,
) : BaseMediaSource() {

    override fun getMediaItem(): MediaItem = mediaItem

    override fun prepareSourceInternal(mediaTransferListener: TransferListener?) = Unit

    // The player asks for this while it waits for the source's timeline, which never comes.
    override fun maybeThrowSourceInfoRefreshError() {
        throw ParserException.createForUnsupportedContainerFeature(
            "No ExoPlayer module plays content type $contentType (${mediaItem.localConfiguration?.uri})",
        )
    }

    override fun createPeriod(id: MediaSource.MediaPeriodId, allocator: Allocator, startPositionUs: Long): MediaPeriod =
        error("A source without a timeline has no periods")

    override fun releasePeriod(mediaPeriod: MediaPeriod) = Unit

    override fun releaseSourceInternal() = Unit
}
