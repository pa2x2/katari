package mihon.video.engine.mpv.proxy.hls

/**
 * Points every reference in an HLS playlist, master or media, through the proxy: the URI lines (variant playlists
 * and segments) and every tag's `URI="…"` attribute (`EXT-X-KEY`, `EXT-X-MAP`, `EXT-X-MEDIA`,
 * `EXT-X-I-FRAME-STREAM-INF`, `EXT-X-SESSION-KEY`, `EXT-X-PART`, `EXT-X-PRELOAD-HINT` and the rest). A reference left
 * alone would reach the host directly, without the source's cookies and headers.
 *
 * [rewrite] gets each reference as written and returns its proxy URL, or null to leave it as it is, such as a `data:`
 * key.
 */
internal fun rewriteHlsPlaylist(playlist: String, rewrite: (String) -> String?): String =
    playlist.split('\n').joinToString("\n") { rawLine ->
        val line = rawLine.removeSuffix("\r")
        val ending = rawLine.substring(line.length)
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> rawLine
            trimmed.startsWith("#") -> URI_ATTRIBUTE.replace(line) { match ->
                val reference = match.groupValues[1]
                rewrite(reference)?.let { "URI=\"$it\"" } ?: match.value
            } + ending
            else -> (rewrite(trimmed) ?: line) + ending
        }
    }

/** Whether [text] is an HLS playlist, whatever its name and content type say. */
internal fun isHlsPlaylist(text: String): Boolean = text.trimStart('﻿', ' ', '\t', '\r', '\n').startsWith("#EXTM3U")

private val URI_ATTRIBUTE = Regex("""URI="([^"]*)"""")
