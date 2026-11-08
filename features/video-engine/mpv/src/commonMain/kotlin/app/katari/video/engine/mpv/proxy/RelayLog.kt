package app.katari.video.engine.mpv.proxy

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

/**
 * What the app's log keeps of the bodies the proxy relays to mpv, so a download that reached mpv cut short can be
 * traced to where it was cut: the host ending a body before its length, mpv's connection breaking before mpv read a
 * body whole, or the host sending bodies without a length, whose cut-off mpv can't tell from their end. [player] names
 * the player in each line. Without [connectionBreaks], broken connections aren't logged, for a player that breaks
 * them on every seek.
 */
internal class RelayLog(private val player: String, private val connectionBreaks: Boolean) {
    private val unsized = ConcurrentHashMap.newKeySet<HeaderSet>()

    /** [body]'s source ended. Returns whether all of the body was relayed. */
    fun sourceEnded(body: RelayedBody): Boolean {
        body.state = RelayedBody.State.Ended
        if (body.expected < 0) {
            if (unsized.add(body.headers)) {
                logcat(LogPriority.INFO) {
                    "$PREFIX ($player): the ${body.headers.logName} comes without a length: $body"
                }
            }
            return true
        }
        if (body.copied >= body.expected) return true
        logcat(LogPriority.WARN) { "$PREFIX ($player): the host ended a body early: $body" }
        return false
    }

    /** The connection [lastBody] was relayed on last broke with [cause]. */
    fun connectionBroke(lastBody: RelayedBody?, cause: IOException) {
        // A timeout only ends an idle connection mpv kept for later, and a failed upstream read is logged as such.
        if (!connectionBreaks || cause is SocketTimeoutException) return
        if (lastBody?.state == RelayedBody.State.UpstreamFailed) return
        logcat(LogPriority.INFO) {
            "$PREFIX ($player): mpv's connection broke ($cause) ${lastBody?.let { "on $it" } ?: "before any body"}"
        }
    }

    private companion object {
        const val PREFIX = "mpv stream proxy"
    }
}

/** A body relayed to mpv, [expected] bytes long or -1 when the host didn't say, of which [copied] were written. */
internal class RelayedBody(val headers: HeaderSet, private val name: String, val expected: Long) {
    var copied = 0L
    var state = State.Copying

    enum class State { Copying, Ended, UpstreamFailed }

    override fun toString(): String {
        val length = if (expected >= 0) "of $expected " else ""
        val progress = when (state) {
            State.Copying -> "while writing"
            State.Ended -> "after writing"
            State.UpstreamFailed -> "after the host failed at"
        }
        return "${headers.logName} $name, $progress $copied ${length}bytes"
    }
}

/** What the proxy writes on one of mpv's connections, with the body it relayed there last. */
internal class MpvConnectionOutput(stream: OutputStream) : BufferedOutputStream(stream) {
    var lastBody: RelayedBody? = null
}

private val HeaderSet.logName: String
    get() = when (this) {
        HeaderSet.Stream -> "stream"
        is HeaderSet.Audio -> "audio track $index"
        is HeaderSet.Subtitle -> "subtitle $index"
    }
