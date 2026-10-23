package eu.kanade.tachiyomi.animesource.model

import fi.iki.elonen.NanoHTTPD
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * A loopback server an extension runs to serve streams its own code produces. The extension puts
 * [PLACEHOLDER_URL] into its video, audio and subtitle URLs; the host starts the server and replaces the
 * placeholder with [url].
 */
open class HttpServer : NanoHTTPD(0) {

    val url: String
        get() = "http://localhost:$listeningPort"

    @Volatile
    private var isRunning = false

    fun isRunning(): Boolean = isRunning

    override fun start() {
        try {
            super.start()
            isRunning = true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to start an extension's http server" }
        }
    }

    override fun stop() {
        super.stop()
        isRunning = false
    }

    companion object {
        const val PLACEHOLDER_URL = "http://localhost:1"
    }
}
