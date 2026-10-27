package mihon.video.engine.mpv.player

import androidx.media3.common.PlaybackException
import `is`.xyz.mpv.MPVLib.MpvError
import mihon.video.engine.mpv.proxy.UpstreamFailure
import java.io.FileNotFoundException
import java.net.SocketTimeoutException

/**
 * Why mpv stopped, as the [PlaybackException] ExoPlayer raises for the same failure, so the player explains both
 * engines' failures alike. mpv only sees what the proxy answered, so the proxy's [upstream] failure is the cause
 * whenever there is one; mpv's own [mpvError] and its last error lines in [log] stand in otherwise.
 */
internal fun mpvPlaybackException(
    upstream: UpstreamFailure?,
    mpvError: Int,
    mpvErrorMessage: String?,
    log: List<String>,
): PlaybackException {
    val mpvDetails = listOfNotNull(mpvErrorMessage).plus(log).joinToString(separator = "\n").ifBlank { null }
    if (upstream != null) return upstream.toPlaybackException(mpvDetails)
    val code = when (mpvError) {
        MpvError.MPV_ERROR_LOADING_FAILED,
        MpvError.MPV_ERROR_UNKNOWN_FORMAT,
        -> PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
        MpvError.MPV_ERROR_NOTHING_TO_PLAY,
        MpvError.MPV_ERROR_UNSUPPORTED,
        MpvError.MPV_ERROR_NOT_IMPLEMENTED,
        -> PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
        MpvError.MPV_ERROR_AO_INIT_FAILED -> PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED
        MpvError.MPV_ERROR_VO_INIT_FAILED -> PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
        else -> PlaybackException.ERROR_CODE_UNSPECIFIED
    }
    return PlaybackException(mpvDetails, null, code)
}

/**
 * The stream ended at [positionMs] of [durationMs] without mpv reporting an error: a connection that dropped
 * mid-stream looks like the end of the file to it.
 */
internal fun mpvEndedEarlyException(
    upstream: UpstreamFailure?,
    positionMs: Long,
    durationMs: Long,
    log: List<String>,
): PlaybackException {
    val details = (listOf("The stream ended at ${positionMs}ms of ${durationMs}ms") + log).joinToString("\n")
    return upstream?.toPlaybackException(details)
        ?: PlaybackException(details, null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
}

private fun UpstreamFailure.toPlaybackException(details: String?): PlaybackException = when (this) {
    // The text ExoPlayer's HTTP data source gives, which leaves out the URL and the tokens it may carry.
    is UpstreamFailure.Status -> PlaybackException(
        listOfNotNull("Response code: $code", details).joinToString("\n"),
        null,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
    )
    is UpstreamFailure.Network -> PlaybackException(
        details,
        cause,
        if (cause is SocketTimeoutException) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        } else {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        },
    )
    is UpstreamFailure.Local -> PlaybackException(
        details,
        cause,
        when (cause) {
            is FileNotFoundException -> PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
            is SecurityException -> PlaybackException.ERROR_CODE_IO_NO_PERMISSION
            else -> PlaybackException.ERROR_CODE_IO_UNSPECIFIED
        },
    )
}
