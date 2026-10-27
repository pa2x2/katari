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
