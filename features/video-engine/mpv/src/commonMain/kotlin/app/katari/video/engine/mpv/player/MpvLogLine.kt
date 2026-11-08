package app.katari.video.engine.mpv.player

/**
 * One line of mpv's log as Katari keeps it. The URLs mpv names are the proxy's, which carry the source's own path and
 * tokens, so they are masked.
 */
internal fun mpvLogLine(prefix: String, text: String): String = "[$prefix] ${text.trim().replace(URL, "<url>")}"

private val URL = Regex("""\bhttps?://\S+""")
