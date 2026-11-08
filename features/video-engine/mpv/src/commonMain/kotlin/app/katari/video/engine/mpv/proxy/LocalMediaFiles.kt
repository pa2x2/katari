package app.katari.video.engine.mpv.proxy

import java.io.Closeable
import java.io.InputStream

/**
 * The downloaded files the proxy serves, named by the URIs the downloader stored: on Android documents of a storage
 * tree (`content://`) or plain paths (`file://`). Reading one that is gone or unreadable throws an [java.io.IOException],
 * a [SecurityException] for one the app may no longer read, or an [IllegalArgumentException] for a URI that names no
 * file.
 */
internal interface LocalMediaFiles {
    /** The name of the file [uri] names, which tells a playlist's format. */
    fun name(uri: String): String

    /**
     * The file [name] next to [uri] in its folder, or null where there is none to name. A downloaded HLS episode is one
     * folder whose playlists name their segments, keys and variant playlists by file name.
     */
    fun sibling(uri: String, name: String): String?

    fun readText(uri: String): String

    /** Opens the bytes [range] asks for, or the whole file. */
    fun open(uri: String, range: ByteRange?): OpenedLocalMedia
}

internal class OpenedLocalMedia(
    val stream: InputStream,
    val length: Long,
    /** Set for a range request, which is answered with 206. */
    val contentRange: String?,
) : Closeable {
    val partial: Boolean get() = contentRange != null

    override fun close() = stream.close()
}
