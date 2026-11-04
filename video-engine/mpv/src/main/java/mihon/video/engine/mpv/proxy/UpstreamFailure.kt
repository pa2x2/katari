package mihon.video.engine.mpv.proxy

import java.io.IOException

/** A request the proxy couldn't serve, kept so the engine can say why mpv stopped. */
sealed interface UpstreamFailure {
    val url: String

    /** The host answered with an error status. */
    data class Status(override val url: String, val code: Int) : UpstreamFailure

    /** The host couldn't be reached or the connection broke. */
    data class Network(override val url: String, val cause: IOException) : UpstreamFailure

    /** A downloaded file couldn't be read. */
    data class Local(override val url: String, val cause: Exception) : UpstreamFailure
}

/**
 * The latest request for the stream itself that the proxy couldn't serve, and whether it has served the stream's media
 * in full since. FFmpeg's HLS demuxer skips a segment that doesn't load, so with the host out of reach it runs through
 * the rest of the playlist and mpv reaches the end of the file as if the episode had finished: an end with nothing
 * served since the failure is the failure's.
 */
internal data class StreamFailure(val failure: UpstreamFailure, val mediaServedSince: Boolean)
