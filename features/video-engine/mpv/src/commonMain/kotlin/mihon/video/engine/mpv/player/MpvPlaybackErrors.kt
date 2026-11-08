package mihon.video.engine.mpv.player

import mihon.video.engine.mpv.client.MpvErrorCode
import mihon.video.engine.mpv.proxy.UpstreamFailure
import mihon.video.engine.spi.player.VideoPlayerError
import java.io.FileNotFoundException
import java.net.SocketTimeoutException

/*
 * Why mpv stopped, in the category ExoPlayer's error for the same failure falls in, so the player explains both
 * engines' failures alike. Each error is named by the code ExoPlayer would have raised, which bug reports show.
 */

/**
 * mpv only sees what the proxy answered, so the proxy's [upstream] failure is the cause whenever there is one; mpv's
 * own [mpvError], one of [MpvErrorCode], and its last error lines in [log] stand in otherwise.
 */
internal fun mpvPlaybackError(
    upstream: UpstreamFailure?,
    mpvError: Int,
    mpvErrorMessage: String?,
    log: List<String>,
): VideoPlayerError {
    val mpvDetails = listOfNotNull(mpvErrorMessage).plus(log).joinToString(separator = "\n").ifBlank { null }
    if (upstream != null) return upstream.toPlaybackError(mpvDetails)
    val code = when (mpvError) {
        MpvErrorCode.LOADING_FAILED,
        MpvErrorCode.UNKNOWN_FORMAT,
        -> ErrorCode.ContainerUnsupported
        MpvErrorCode.NOTHING_TO_PLAY,
        MpvErrorCode.UNSUPPORTED,
        MpvErrorCode.NOT_IMPLEMENTED,
        -> ErrorCode.FormatUnsupported
        MpvErrorCode.AO_INIT_FAILED -> ErrorCode.AudioTrackInitFailed
        MpvErrorCode.VO_INIT_FAILED -> ErrorCode.DecoderInitFailed
        else -> ErrorCode.Unspecified
    }
    return code.error(mpvDetails, null)
}

/**
 * The stream ended at [positionMs] of [durationMs] (null when unknown) without mpv reporting an error: a connection
 * that dropped mid-stream, and segments that didn't load, look like the end of the file to it.
 */
internal fun mpvEndedEarlyError(
    upstream: UpstreamFailure?,
    positionMs: Long,
    durationMs: Long?,
    log: List<String>,
): VideoPlayerError {
    val of = if (durationMs == null) "" else " of ${durationMs}ms"
    val details = (listOf("The stream ended at ${positionMs}ms$of") + log).joinToString("\n")
    return upstream?.toPlaybackError(details) ?: ErrorCode.IoUnspecified.error(details, null)
}

/** The instance couldn't start, which ends the player's session for good. */
internal fun mpvStartError(): VideoPlayerError = ErrorCode.FailedRuntimeCheck.error(
    "mpv couldn't start: its add-on didn't load or its stream proxy didn't open",
    null,
)

private fun UpstreamFailure.toPlaybackError(details: String?): VideoPlayerError = when (this) {
    // The text ExoPlayer's HTTP data source gives, which leaves out the URL and the tokens it may carry.
    is UpstreamFailure.Status ->
        ErrorCode.BadHttpStatus.error(listOfNotNull("Response code: $code", details).joinToString("\n"), null)
    is UpstreamFailure.Network -> if (cause is SocketTimeoutException) {
        ErrorCode.ConnectionTimeout.error(details, cause)
    } else {
        ErrorCode.ConnectionFailed.error(details, cause)
    }
    is UpstreamFailure.Local -> when (cause) {
        is FileNotFoundException -> ErrorCode.FileNotFound
        is SecurityException -> ErrorCode.NoPermission
        else -> ErrorCode.IoUnspecified
    }.error(details, cause)
}

/** The ExoPlayer error codes mpv's failures are told as. */
private enum class ErrorCode(val codeName: String, val category: VideoPlayerError.Category) {
    Unspecified("ERROR_CODE_UNSPECIFIED", VideoPlayerError.Category.Other),
    FailedRuntimeCheck("ERROR_CODE_FAILED_RUNTIME_CHECK", VideoPlayerError.Category.Other),
    IoUnspecified("ERROR_CODE_IO_UNSPECIFIED", VideoPlayerError.Category.LoadFailed),
    ConnectionFailed("ERROR_CODE_IO_NETWORK_CONNECTION_FAILED", VideoPlayerError.Category.ConnectionFailed),
    ConnectionTimeout("ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT", VideoPlayerError.Category.ConnectionTimedOut),
    BadHttpStatus("ERROR_CODE_IO_BAD_HTTP_STATUS", VideoPlayerError.Category.Rejected),
    FileNotFound("ERROR_CODE_IO_FILE_NOT_FOUND", VideoPlayerError.Category.Rejected),
    NoPermission("ERROR_CODE_IO_NO_PERMISSION", VideoPlayerError.Category.Rejected),
    ContainerUnsupported("ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED", VideoPlayerError.Category.Unplayable),
    DecoderInitFailed("ERROR_CODE_DECODER_INIT_FAILED", VideoPlayerError.Category.Unplayable),
    FormatUnsupported("ERROR_CODE_DECODING_FORMAT_UNSUPPORTED", VideoPlayerError.Category.Unplayable),
    AudioTrackInitFailed("ERROR_CODE_AUDIO_TRACK_INIT_FAILED", VideoPlayerError.Category.Other),
    ;

    fun error(message: String?, cause: Throwable?) = VideoPlayerError(category, codeName, message, cause)
}
