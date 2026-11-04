package mihon.video.engine.spi.media3

import androidx.media3.common.PlaybackException
import mihon.video.engine.spi.player.VideoPlayerError

internal fun PlaybackException.toVideoPlayerError(): VideoPlayerError = VideoPlayerError(
    category = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> VideoPlayerError.Category.ConnectionFailed
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> VideoPlayerError.Category.ConnectionTimedOut
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
        -> VideoPlayerError.Category.Rejected
        in INPUT_OUTPUT_ERROR_CODES -> VideoPlayerError.Category.LoadFailed
        in PARSING_AND_DECODING_ERROR_CODES -> VideoPlayerError.Category.Unplayable
        else -> VideoPlayerError.Category.Other
    },
    codeName = errorCodeName,
    message = message,
    cause = cause,
)

// Media3 groups input/output errors in 2xxx, parsing errors in 3xxx and decoding errors in 4xxx.
private val INPUT_OUTPUT_ERROR_CODES = 2000..2999
private val PARSING_AND_DECODING_ERROR_CODES = 3000..4999
