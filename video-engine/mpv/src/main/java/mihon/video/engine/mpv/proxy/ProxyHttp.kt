package mihon.video.engine.mpv.proxy

import java.io.BufferedInputStream
import java.io.IOException
import java.io.OutputStream

/** One HTTP/1.1 request as mpv's FFmpeg sends it: no bodies, so only the request line and the headers. */
internal class ProxyRequest(
    val method: String,
    val path: String,
    val query: String?,
    /** Header names in lower case. */
    val headers: Map<String, String>,
) {
    val keepAlive: Boolean
        get() = !headers["connection"].equals("close", ignoreCase = true)

    companion object {
        /** Null at the end of the connection. */
        fun read(input: BufferedInputStream): ProxyRequest? {
            val requestLine = readLine(input) ?: return null
            val parts = requestLine.split(' ')
            if (parts.size != 3) throw IOException("Bad request line")
            val headers = buildMap {
                while (true) {
                    val line = readLine(input) ?: throw IOException("Request ended inside its headers")
                    if (line.isEmpty()) break
                    val colon = line.indexOf(':')
                    if (colon > 0) put(line.substring(0, colon).trim().lowercase(), line.substring(colon + 1).trim())
                }
            }
            val target = parts[1]
            return ProxyRequest(
                method = parts[0],
                path = target.substringBefore('?'),
                query = target.substringAfter('?', "").takeIf { '?' in target },
                headers = headers,
            )
        }

        private fun readLine(input: BufferedInputStream): String? {
            val line = StringBuilder()
            while (true) {
                val byte = input.read()
                if (byte == -1) return if (line.isEmpty()) null else throw IOException("Connection closed mid-line")
                if (byte == '\n'.code) return line.toString().removeSuffix("\r")
                if (line.length >= MAX_LINE) throw IOException("Request line too long")
                line.append(byte.toChar())
            }
        }

        private const val MAX_LINE = 16 * 1024
    }
}

/** A requested byte range, `bytes=<first>-[<last>]`; suffix ranges aren't used by FFmpeg and are served whole. */
internal data class ByteRange(val first: Long, val last: Long?) {
    companion object {
        fun parse(header: String?): ByteRange? {
            val match = header?.let(RANGE::matchEntire) ?: return null
            val first = match.groupValues[1].toLongOrNull() ?: return null
            val last = match.groupValues[2].takeIf { it.isNotEmpty() }?.toLongOrNull()
            return if (last != null && last < first) null else ByteRange(first, last)
        }

        private val RANGE = Regex("""bytes=(\d+)-(\d*)""")
    }
}

internal fun OutputStream.writeHead(code: Int, reason: String, headers: List<Pair<String, String>>) {
    val head = buildString {
        append("HTTP/1.1 ").append(code).append(' ').append(reason.ifEmpty { "Status" }).append("\r\n")
        headers.forEach { (name, value) -> append(name).append(": ").append(value).append("\r\n") }
        append("\r\n")
    }
    write(head.toByteArray(Charsets.ISO_8859_1))
}

internal fun OutputStream.writeText(code: Int, reason: String, contentType: String, body: String, keepAlive: Boolean) {
    val bytes = body.toByteArray()
    writeHead(
        code,
        reason,
        listOf(
            "Content-Type" to contentType,
            "Content-Length" to bytes.size.toString(),
            "Connection" to if (keepAlive) "keep-alive" else "close",
        ),
    )
    write(bytes)
    flush()
}
