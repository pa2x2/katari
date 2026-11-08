package app.katari.sync.server.http

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall

/**
 * What a call looked like where it reached the outermost trusted proxy. Behind reverse proxies the connection's peer is
 * the nearest proxy, so with [trustedProxies] set the facts come from X-Forwarded-For and X-Forwarded-Proto, where each
 * proxy appends what it saw: the entry [trustedProxies] from the right is the outermost trusted proxy's. Entries left
 * of it come from the client, who can write anything there, so they are never used.
 */
internal fun ApplicationCall.clientAddress(trustedProxies: Int): String =
    forwarded(HttpHeaders.XForwardedFor, trustedProxies) ?: request.local.remoteAddress

/**
 * Whether the browser reached the server over HTTPS. The server speaks plain HTTP itself, so only a trusted proxy can
 * say so; a proxy that sets the header rather than appending to it leaves one entry, which is then the one read.
 */
internal fun ApplicationCall.cameOverHttps(trustedProxies: Int): Boolean {
    if (trustedProxies == 0) return request.local.scheme == "https"
    val proto = forwarded(HttpHeaders.XForwardedProto, trustedProxies)
        ?: request.headers.getAll(HttpHeaders.XForwardedProto)?.singleOrNull()?.trim()
    return proto.equals("https", ignoreCase = true)
}

/**
 * The address the browser reached the server at, such as `https://sync.example.com`: the page and the sync API share
 * it, so it is the one apps use too. The host is the Host header as the browser sent it, port included, so behind a
 * proxy it is right only when the proxy passes Host on unchanged.
 */
internal fun ApplicationCall.browserAddress(trustedProxies: Int): String {
    val scheme = if (cameOverHttps(trustedProxies)) "https" else "http"
    val host = request.headers[HttpHeaders.Host] ?: "${request.local.serverHost}:${request.local.serverPort}"
    return "$scheme://$host"
}

private fun ApplicationCall.forwarded(header: String, trustedProxies: Int): String? {
    if (trustedProxies == 0) return null
    val entries = request.headers.getAll(header).orEmpty()
        .flatMap { it.split(',') }
        .map(String::trim)
        .filter(String::isNotEmpty)
    return entries.getOrNull(entries.size - trustedProxies)
}
