package mihon.video.engine.mpv.player

import mihon.video.engine.mpv.client.MpvLogLevel

/** mpv's latest error lines, for the details of a failure. Written on mpv's event thread, read on the UI thread. */
internal class MpvErrorLog {
    private val lines = ArrayDeque<String>()

    fun add(prefix: String, level: MpvLogLevel, text: String) {
        if (level > MpvLogLevel.Error) return
        synchronized(lines) {
            lines.addLast(mpvLogLine(prefix, text))
            while (lines.size > LINES) lines.removeFirst()
        }
    }

    fun recent(): List<String> = synchronized(lines) { lines.toList() }

    private companion object {
        const val LINES = 5
    }
}
