package eu.kanade.tachiyomi.ui.video.player

import androidx.media3.common.PlaybackException
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Why an episode couldn't open or stopped playing, grouped by what the viewer can do about it. [details] keeps the
 * technical text, such as the exception or the player's error code, for a bug report.
 */
internal data class VideoPlayerProblem(
    val cause: Cause,
    val details: String? = null,
) {
    /**
     * [offersWebView] is false where the source's page can't help: no connection, no extension to ask, or a torrent the
     * source answered with on purpose. [switchHelps] marks the causes another server, dub, quality or stream may avoid,
     * whose screens then point at that choice instead of [explanation].
     */
    enum class Cause(
        val title: StringResource,
        val explanation: StringResource,
        val offersWebView: Boolean = true,
        val switchHelps: Boolean = false,
    ) {
        Offline(MR.strings.anime_problem_offline, MR.strings.anime_problem_offline_explanation, offersWebView = false),
        Unreachable(MR.strings.anime_problem_unreachable, MR.strings.anime_problem_unreachable_explanation),
        TimedOut(
            MR.strings.anime_problem_timed_out,
            MR.strings.anime_problem_timed_out_explanation,
            switchHelps = true,
        ),
        SourceFailed(MR.strings.anime_problem_source_failed, MR.strings.anime_problem_source_failed_explanation),
        StreamRejected(MR.strings.anime_problem_stream_rejected, MR.strings.anime_problem_stream_rejected_explanation),
        NoStreams(
            MR.strings.anime_problem_no_streams,
            MR.strings.anime_problem_no_streams_explanation,
            switchHelps = true,
        ),
        TorrentOnly(
            MR.strings.anime_problem_torrent_only,
            MR.strings.anime_problem_torrent_only_explanation,
            offersWebView = false,
            switchHelps = true,
        ),
        Unplayable(
            MR.strings.anime_problem_unplayable,
            MR.strings.anime_problem_unplayable_explanation,
            switchHelps = true,
        ),
        NotFound(MR.strings.anime_problem_not_found, MR.strings.anime_problem_not_found_explanation),
        SourceMissing(
            MR.strings.anime_problem_source_missing,
            MR.strings.anime_problem_source_missing_explanation,
            offersWebView = false,
        ),
        Stopped(MR.strings.anime_problem_stopped, MR.strings.anime_problem_stopped_explanation, switchHelps = true),
    }
}

internal fun ResolveVideoStream.Reason.toProblem(): VideoPlayerProblem = when (this) {
    ResolveVideoStream.Reason.VideoNotFound,
    ResolveVideoStream.Reason.EpisodeNotFound,
    ResolveVideoStream.Reason.EpisodeMismatch,
    -> VideoPlayerProblem(VideoPlayerProblem.Cause.NotFound)
    ResolveVideoStream.Reason.SourceLoadTimeout,
    ResolveVideoStream.Reason.StreamFetchTimeout,
    -> VideoPlayerProblem(VideoPlayerProblem.Cause.TimedOut)
    ResolveVideoStream.Reason.SourceNotFound -> VideoPlayerProblem(VideoPlayerProblem.Cause.SourceMissing)
    ResolveVideoStream.Reason.NoStreams -> VideoPlayerProblem(VideoPlayerProblem.Cause.NoStreams)
    ResolveVideoStream.Reason.TorrentOnly -> VideoPlayerProblem(VideoPlayerProblem.Cause.TorrentOnly)
    ResolveVideoStream.Reason.OfflineNoDownload -> VideoPlayerProblem(VideoPlayerProblem.Cause.Offline)
    is ResolveVideoStream.Reason.StreamFetchFailed -> VideoPlayerProblem(
        cause = generateSequence(cause) { it.cause }.firstNotNullOfOrNull(::networkCause)
            ?: VideoPlayerProblem.Cause.SourceFailed,
        details = cause.describe(),
    )
}

internal fun PlaybackException.toProblem(): VideoPlayerProblem = VideoPlayerProblem(
    cause = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> VideoPlayerProblem.Cause.Unreachable
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> VideoPlayerProblem.Cause.TimedOut
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
        -> VideoPlayerProblem.Cause.StreamRejected
        in PARSING_AND_DECODING_ERROR_CODES -> VideoPlayerProblem.Cause.Unplayable
        else -> VideoPlayerProblem.Cause.Stopped
    },
    details = buildList {
        add(errorCodeName)
        message?.takeIf { it.isNotBlank() }?.let(::add)
        generateSequence(cause) { it.cause }.forEach { add(it.describe()) }
    }.distinct().joinToString(separator = "\n"),
)

private fun networkCause(error: Throwable): VideoPlayerProblem.Cause? = when (error) {
    is UnknownHostException, is ConnectException, is NoRouteToHostException -> VideoPlayerProblem.Cause.Unreachable
    is SocketTimeoutException -> VideoPlayerProblem.Cause.TimedOut
    else -> null
}

private fun Throwable.describe(): String =
    listOfNotNull(this::class.qualifiedName ?: this::class.simpleName, message?.takeIf { it.isNotBlank() })
        .joinToString(": ")

// Media3 groups parsing errors in 3xxx and decoding errors in 4xxx.
internal val PARSING_AND_DECODING_ERROR_CODES = 3000..4999
