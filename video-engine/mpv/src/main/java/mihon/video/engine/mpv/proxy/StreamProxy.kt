package mihon.video.engine.mpv.proxy

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import logcat.LogPriority
import mihon.video.engine.dash.UnsupportedDashManifestException
import mihon.video.engine.dash.routeDashManifest
import mihon.video.engine.mpv.proxy.hls.FetchedPlaylist
import mihon.video.engine.mpv.proxy.hls.HlsVariantPick
import mihon.video.engine.mpv.proxy.hls.HlsVariantSet
import mihon.video.engine.mpv.proxy.hls.isHlsPlaylist
import mihon.video.engine.mpv.proxy.hls.loadHlsVariantSets
import mihon.video.engine.mpv.proxy.hls.parseHlsMasterPlaylist
import mihon.video.engine.mpv.proxy.hls.rewriteHlsPlaylist
import mihon.video.engine.mpv.quality.MpvBandwidthMeter
import mihon.video.engine.spi.loading.VideoRequestHeaders
import mihon.video.engine.spi.loading.resolvedRequest
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import tachiyomi.core.common.util.system.logcat
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The one way mpv reaches a stream: a loopback HTTP server for one playback, so every request goes through Katari's
 * OkHttp client with the same headers ExoPlayer sends ([VideoRequestHeaders]), cookies and Cloudflare handling
 * included, and downloaded episodes are read from their storage tree. mpv's own HTTP stack can do neither.
 *
 * It listens on 127.0.0.1 only, on a random port, and answers only paths that start with an unguessable token.
 * Playlists and manifests are rewritten so their references come back here ([rewriteHlsPlaylist],
 * [routeDashManifest]). The variants of the stream's HLS master playlist that can switch at any segment reach mpv as
 * one per set, whose segments the proxy serves from the variant chosen for each ([variantSets]). Upstream failures are
 * kept in [failures] for the engine's error report. It runs until [close].
 */
internal class StreamProxy(
    private val context: Context,
    private val client: OkHttpClient,
    stream: VideoStream,
    private val headers: VideoRequestHeaders,
    private val bandwidthMeter: MpvBandwidthMeter,
    /** How far ahead of the playback position mpv has buffered, which Auto's choice of a segment's variant weighs. */
    private val bufferedAheadMs: () -> Long,
    /** What the player asks to play from a set of variants now; asked for every segment. */
    private val variantPick: (HlsVariantSet) -> HlsVariantPick,
) : Closeable {

    private val subtitles = stream.subtitles
    private val audioTracks = stream.audioTracks

    // What the stream and its audio tracks are, as the host found out before playing: a manifest served as text/plain
    // under a name without .mpd would otherwise reach mpv unrewritten, with its segments bypassing the proxy.
    private val declaredManifests: Map<Pair<HeaderSet, HttpUrl>, ManifestKind> = buildMap {
        stream.type.manifestKind()?.let { kind ->
            stream.resolvedRequest.url.toHttpUrlOrNull()?.let { put(HeaderSet.Stream to it, kind) }
        }
        audioTracks.forEachIndexed { index, track ->
            val kind = track.type.manifestKind() ?: return@forEachIndexed
            track.request.url.toHttpUrlOrNull()?.let { put(HeaderSet.Audio(index) to it, kind) }
        }
    }
    private val server = ServerSocket(0, BACKLOG, InetAddress.getByName(LOOPBACK))
    private val routes = ProxyRoutes(server.localPort, newToken())
    private val executor: ExecutorService = Executors.newCachedThreadPool { task ->
        Thread(task, "mpv-stream-proxy").apply { isDaemon = true }
    }
    private val sockets = ConcurrentHashMap.newKeySet<Socket>()
    private val calls = ConcurrentHashMap.newKeySet<Call>()

    @Volatile
    private var closed = false

    val failures: MutableList<UpstreamFailure> = CopyOnWriteArrayList()

    /** The sets of the stream's HLS variants the proxy switches between, once mpv opened the master playlist. */
    @Volatile
    var variantSets: List<HlsVariantSet> = emptyList()
        private set

    @Volatile
    private var variantSetPlaylists: List<String> = emptyList()

    /** What mpv opens for the stream. */
    val streamUrl: String = proxied(stream.resolvedRequest.url, HeaderSet.Stream)

    /** What mpv opens for each external subtitle, in the order of the subtitles given. */
    val subtitleUrls: List<String> = subtitles.mapIndexed { index, subtitle ->
        proxied(subtitle.request.url, HeaderSet.Subtitle(index))
    }

    /** What mpv opens for each external audio track, in the order of the tracks given. */
    val audioUrls: List<String> = audioTracks.mapIndexed { index, track ->
        proxied(track.request.url, HeaderSet.Audio(index))
    }

    init {
        executor.execute(::accept)
    }

    override fun close() {
        closed = true
        runCatching { server.close() }
        calls.forEach(Call::cancel)
        sockets.forEach { runCatching { it.close() } }
        executor.shutdownNow()
    }

    private fun proxied(url: String, headerSet: HeaderSet): String {
        val uri = Uri.parse(url)
        return when (uri.scheme?.lowercase()) {
            "http", "https" -> url.toHttpUrlOrNull()?.let { routes.remote(headerSet, it) } ?: url
            "content", "file" -> routes.local(url, localName(uri))
            // Anything else (rtmp, rtsp…) is left to mpv, which verifies TLS itself.
            else -> url
        }
    }

    private fun accept() {
        while (!closed) {
            val socket = try {
                server.accept()
            } catch (_: IOException) {
                break
            }
            sockets += socket
            executor.execute {
                try {
                    socket.soTimeout = IDLE_TIMEOUT_MS
                    serve(socket)
                } catch (_: IOException) {
                    // mpv closed the connection, for example to seek.
                } finally {
                    sockets -= socket
                    runCatching { socket.close() }
                }
            }
        }
    }

    private fun serve(socket: Socket) {
        val input = BufferedInputStream(socket.getInputStream())
        val output = socket.getOutputStream().buffered()
        while (!closed) {
            val request = ProxyRequest.read(input) ?: return
            val keepAlive = when (val target = routes.parse(request.path, request.query)) {
                null -> {
                    output.writeText(404, "Not Found", "text/plain", "", keepAlive = false)
                    false
                }
                is ProxyTarget.Remote -> serveRemote(request, target, output)
                is ProxyTarget.Local -> serveLocal(request, target, output)
                is ProxyTarget.VariantSet -> serveVariantSet(request, target, output)
            }
            output.flush()
            if (!keepAlive || !request.keepAlive) return
        }
    }

    /** Returns whether the connection can take another request. */
    private fun serveRemote(request: ProxyRequest, target: ProxyTarget.Remote, output: OutputStream): Boolean {
        val call = client.newCall(
            upstreamRequest(target.url, target.headers, request.headers["range"], head = request.method == "HEAD"),
        )
        calls += call
        try {
            val response = try {
                call.execute()
            } catch (e: IOException) {
                if (!closed) recordFailure(UpstreamFailure.Network(target.url.toString(), e))
                output.writeText(502, "Bad Gateway", "text/plain", "", keepAlive = false)
                return false
            }
            return response.use { relay(request, target, it, output) }
        } finally {
            calls -= call
        }
    }

    private fun relay(
        request: ProxyRequest,
        target: ProxyTarget.Remote,
        response: Response,
        output: OutputStream,
    ): Boolean {
        val url = target.url.toString()
        if (!response.isSuccessful) {
            recordFailure(UpstreamFailure.Status(url, response.code))
            output.writeText(response.code, response.message, "text/plain", "", keepAlive = false)
            return false
        }
        val contentType = response.header("Content-Type")
        val manifest = declaredManifests[target.headers to target.url]
            ?: manifestKind(target.url.encodedPath, contentType)
            ?: response.sniffHlsPlaylist()
        if (manifest != null && request.method != "HEAD") {
            val text = try {
                response.decoded(response.body.source()).asResponseBody(response.body.contentType()).string()
            } catch (e: IOException) {
                recordFailure(UpstreamFailure.Network(url, e))
                output.writeText(502, "Bad Gateway", "text/plain", "", keepAlive = false)
                return false
            }
            val finalUrl = response.request.url
            val rewritten = when (manifest) {
                ManifestKind.Hls -> withVariantSets(target, finalUrl, text) ?: rewriteHlsPlaylist(text) { reference ->
                    routes.remoteReference(target.headers, finalUrl, reference)
                }
                ManifestKind.Dash -> try {
                    routeDashManifest(text, finalUrl) { url -> routes.remoteReference(target.headers, url) }
                } catch (_: UnsupportedDashManifestException) {
                    // Not a manifest after all, such as an error page: mpv reports what it got.
                    text
                }
            }
            output.writeText(200, "OK", contentType ?: manifest.contentType, rewritten, keepAlive = true)
            return true
        }

        return relayBody(
            request,
            url,
            response,
            output,
            response.code,
            response.message,
            response.header("Content-Range"),
        )
    }

    /**
     * Relays [response]'s body as it came, with [code] and [contentRange] as the client's view of it. [url] is what was
     * asked for, which a failure is reported under.
     */
    private fun relayBody(
        request: ProxyRequest,
        url: String,
        response: Response,
        output: OutputStream,
        code: Int,
        reason: String,
        contentRange: String?,
    ): Boolean {
        val length = response.body.contentLength()
        output.writeHead(
            code,
            reason,
            buildList {
                response.header("Content-Type")?.let { add("Content-Type" to it) }
                response.header("Content-Encoding")?.let { add("Content-Encoding" to it) }
                if (length >= 0) add("Content-Length" to length.toString())
                contentRange?.let { add("Content-Range" to it) }
                add("Accept-Ranges" to (response.header("Accept-Ranges") ?: "bytes"))
                add("Connection" to if (length >= 0) "keep-alive" else "close")
            },
        )
        if (request.method == "HEAD") return length >= 0
        copy(response.body.byteStream(), output, bandwidthMeter) { recordFailure(UpstreamFailure.Network(url, it)) }
        return length >= 0
    }

    /**
     * The master playlist mpv gets for the stream's own [text] fetched from [url], with its sets of variants that can
     * switch at any segment as one variant each; null when it isn't the stream's master playlist or has no such set.
     */
    private fun withVariantSets(target: ProxyTarget.Remote, url: HttpUrl, text: String): String? {
        if (target.headers != HeaderSet.Stream) return null
        val master = parseHlsMasterPlaylist(text) ?: return null
        val result = loadHlsVariantSets(
            master = master,
            base = url,
            executor = executor,
            fetch = ::fetchPlaylist,
            route = { base, reference -> routes.remoteReference(HeaderSet.Stream, base, reference) },
            setUrl = routes::variantSetPlaylist,
            segmentUrl = routes::variantSetSegment,
        ) ?: return null
        variantSetPlaylists = result.setPlaylists
        variantSets = result.sets
        return result.text
    }

    /** A variant's media playlist, with the stream's headers; null when it doesn't load. */
    private fun fetchPlaylist(url: HttpUrl): FetchedPlaylist? {
        val call = client.newCall(upstreamRequest(url, HeaderSet.Stream, range = null))
        calls += call
        return try {
            call.execute().use { response ->
                if (!response.isSuccessful) return null
                FetchedPlaylist(response.request.url, response.decoded(response.body.source()).readUtf8())
            }
        } catch (_: IOException) {
            null
        } finally {
            calls -= call
        }
    }

    private fun serveVariantSet(request: ProxyRequest, target: ProxyTarget.VariantSet, output: OutputStream): Boolean {
        val set = variantSets.getOrNull(target.set)
        val index = target.segment
        if (set == null || (index != null && index >= set.segmentCount)) {
            output.writeText(404, "Not Found", "text/plain", "", keepAlive = false)
            return false
        }
        if (index == null) {
            output.writeText(200, "OK", ManifestKind.Hls.contentType, variantSetPlaylists[target.set], keepAlive = true)
            return true
        }
        val asked = ByteRange.parse(request.headers["range"])
        val variant = set.variantForSegment(
            index,
            continues = (asked?.first ?: 0L) > 0L,
            pick = variantPick(set),
            bitrateEstimate = bandwidthMeter.bitrateEstimate,
            bufferedAheadMs = bufferedAheadMs(),
        )
        val loaded = set.variants[variant]
        val segment = loaded.playlist.segments[index]
        val url = loaded.url.resolve(segment.uri) ?: run {
            output.writeText(404, "Not Found", "text/plain", "", keepAlive = false)
            return false
        }
        // A segment that is part of a file is served as a file of its own: mpv's range is within the segment.
        val part = segment.byteRange
        val range = if (part == null) {
            request.headers["range"]
        } else {
            val first = part.first + (asked?.first ?: 0L)
            val last = asked?.last?.let { minOf(part.last, part.first + it) } ?: part.last
            "bytes=$first-$last"
        }
        val call = client.newCall(upstreamRequest(url, HeaderSet.Stream, range, head = request.method == "HEAD"))
        calls += call
        try {
            val response = try {
                call.execute()
            } catch (e: IOException) {
                if (!closed) recordFailure(UpstreamFailure.Network(url.toString(), e))
                output.writeText(502, "Bad Gateway", "text/plain", "", keepAlive = false)
                return false
            }
            return response.use {
                when {
                    !it.isSuccessful || (part != null && it.code != PARTIAL_CONTENT) -> {
                        recordFailure(UpstreamFailure.Status(url.toString(), it.code))
                        output.writeText(BAD_GATEWAY, "Bad Gateway", "text/plain", "", keepAlive = false)
                        false
                    }
                    part == null ->
                        relayBody(request, url.toString(), it, output, it.code, it.message, it.header("Content-Range"))
                    asked == null -> relayBody(request, url.toString(), it, output, OK, "OK", contentRange = null)
                    else -> {
                        val first = asked.first
                        val last = first + it.body.contentLength() - 1
                        val total = part.last - part.first + 1
                        relayBody(
                            request,
                            url.toString(),
                            it,
                            output,
                            PARTIAL_CONTENT,
                            "Partial Content",
                            "bytes $first-$last/$total",
                        )
                    }
                }
            }
        } finally {
            calls -= call
        }
    }

    private fun serveLocal(request: ProxyRequest, target: ProxyTarget.Local, output: OutputStream): Boolean {
        val uri = localFile(target) ?: run {
            output.writeText(404, "Not Found", "text/plain", "", keepAlive = false)
            return false
        }
        val media = LocalMedia(context, uri)
        try {
            if (target.name.isHlsPlaylistName()) {
                val text = media.readText()
                val rewritten = rewriteHlsPlaylist(text) { reference -> localReference(media.uri, reference) }
                output.writeText(200, "OK", ManifestKind.Hls.contentType, rewritten, keepAlive = true)
                return true
            }
            media.open(ByteRange.parse(request.headers["range"])).use { opened ->
                output.writeHead(
                    if (opened.partial) 206 else 200,
                    if (opened.partial) "Partial Content" else "OK",
                    buildList {
                        // FFmpeg picks its DASH demuxer by the type; downloaded manifests aren't rewritten.
                        if (target.name.endsWith(".mpd", ignoreCase = true)) {
                            add("Content-Type" to ManifestKind.Dash.contentType)
                        }
                        add("Content-Length" to opened.length.toString())
                        opened.contentRange?.let { add("Content-Range" to it) }
                        add("Accept-Ranges" to "bytes")
                        add("Connection" to "keep-alive")
                    },
                )
                if (request.method != "HEAD") {
                    copy(opened.stream, output, meter = null) {
                        recordFailure(UpstreamFailure.Local(uri.toString(), it))
                    }
                }
            }
            return true
        } catch (e: IOException) {
            recordFailure(UpstreamFailure.Local(uri.toString(), e))
        } catch (e: SecurityException) {
            recordFailure(UpstreamFailure.Local(uri.toString(), e))
        } catch (e: IllegalArgumentException) {
            recordFailure(UpstreamFailure.Local(uri.toString(), e))
        }
        output.writeText(404, "Not Found", "text/plain", "", keepAlive = false)
        return false
    }

    /**
     * The file a local URL names: its own URI under its own name, else the sibling a downloaded DASH manifest's relative
     * reference named, which mpv resolved against the manifest's URL. Only a plain file name in the same folder is
     * served.
     */
    private fun localFile(target: ProxyTarget.Local): Uri? {
        val uri = Uri.parse(target.uri)
        if (target.name == routes.localName(localName(uri))) return uri
        if (!SIBLING_NAME.matches(target.name) || target.name.all { it == '.' }) return null
        return LocalMedia.sibling(uri, target.name)
    }

    /** A reference in a downloaded playlist: a sibling file of the playlist, or a remote URL it kept. */
    private fun localReference(playlist: Uri, reference: String): String? {
        val parsed = Uri.parse(reference)
        return when (parsed.scheme?.lowercase()) {
            "http", "https" -> proxied(reference, HeaderSet.Stream)
            null -> {
                val name = Uri.decode(reference.substringBefore('?').substringBefore('#'))
                LocalMedia.sibling(playlist, name)?.let { routes.local(it.toString(), name) }
            }
            else -> null
        }
    }

    /**
     * Copies [source] to [output], with [meter] timing the reads of a network source; a read failure is the
     * upstream's, a write failure means mpv went away.
     */
    private fun copy(
        source: InputStream,
        output: OutputStream,
        meter: MpvBandwidthMeter?,
        onUpstreamFailure: (IOException) -> Unit,
    ) {
        val buffer = ByteArray(BUFFER_SIZE)
        source.use {
            while (!closed) {
                val read = try {
                    if (meter != null) meter.measure { source.read(buffer) } else source.read(buffer)
                } catch (e: IOException) {
                    if (!closed) onUpstreamFailure(e)
                    throw e
                }
                if (read < 0) break
                output.write(buffer, 0, read)
            }
        }
    }

    private fun upstreamRequest(url: HttpUrl, headerSet: HeaderSet, range: String?, head: Boolean = false): Request =
        Request.Builder()
            .url(url)
            .apply {
                requestHeaders(headerSet).forEach { (name, value) -> header(name, value) }
                header(VideoRequestHeaders.USER_AGENT, headers.userAgent)
                range?.let { header("Range", it) }
                if (head) head()
            }
            .build()

    private fun requestHeaders(headerSet: HeaderSet): Map<String, String> = when (headerSet) {
        HeaderSet.Stream -> headers.stream
        is HeaderSet.Subtitle ->
            headers.stream +
                subtitles.getOrNull(headerSet.index)?.let { headers.trackExtras(it.request.url) }.orEmpty()
        is HeaderSet.Audio ->
            headers.stream +
                audioTracks.getOrNull(headerSet.index)?.let { headers.trackExtras(it.request.url) }.orEmpty()
    }

    private fun recordFailure(failure: UpstreamFailure) {
        logcat(LogPriority.WARN) { "mpv stream proxy: $failure" }
        failures += failure
    }

    private fun Response.sniffHlsPlaylist(): ManifestKind? = ManifestKind.Hls.takeIf {
        runCatching {
            decoded(body.source().peek()).use { start ->
                start.request(SNIFF_BYTES)
                isHlsPlaylist(start.buffer.readUtf8(minOf(start.buffer.size, SNIFF_BYTES)))
            }
        }.getOrDefault(false)
    }

    private fun localName(uri: Uri): String = uri.lastPathSegment.orEmpty().substringAfterLast('/')

    private companion object {
        const val LOOPBACK = "127.0.0.1"
        const val BACKLOG = 16
        const val IDLE_TIMEOUT_MS = 60_000
        const val BUFFER_SIZE = 64 * 1024
        const val SNIFF_BYTES = 16L
        const val OK = 200
        const val PARTIAL_CONTENT = 206
        const val BAD_GATEWAY = 502
        val SIBLING_NAME = Regex("[A-Za-z0-9._-]+")

        fun newToken(): String {
            val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}

private enum class ManifestKind(val contentType: String) {
    Hls("application/vnd.apple.mpegurl"),
    Dash("application/dash+xml"),
}

private fun VideoStreamType.manifestKind(): ManifestKind? = when (this) {
    VideoStreamType.HLS -> ManifestKind.Hls
    VideoStreamType.DASH -> ManifestKind.Dash
    VideoStreamType.PROGRESSIVE, VideoStreamType.UNKNOWN -> null
}

private fun manifestKind(path: String, contentType: String?): ManifestKind? {
    val type = contentType.orEmpty().lowercase()
    return when {
        "mpegurl" in type || path.isHlsPlaylistName() -> ManifestKind.Hls
        "dash+xml" in type || path.endsWith(".mpd", ignoreCase = true) -> ManifestKind.Dash
        else -> null
    }
}

private fun String.isHlsPlaylistName(): Boolean =
    endsWith(".m3u8", ignoreCase = true) || endsWith(".m3u", ignoreCase = true)
