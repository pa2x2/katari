package app.katari.sync.server.http

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall

/**
 * The address a call came from, which failed sign-ins are limited by. Behind reverse proxies the connection's peer is
 * the nearest proxy, so with [trustedProxies] set the address comes from X-Forwarded-For, where each proxy appends the
 * address it was reached from: the entry [trustedProxies] from the right is the one the outermost trusted proxy saw.
 * Entries left of it come from the client, who can write anything there, so they are never used.
 */
internal fun ApplicationCall.clientAddress(trustedProxies: Int): String {
    val peer = request.local.remoteAddress
    if (trustedProxies == 0) return peer
    val forwarded = request.headers.getAll(HttpHeaders.XForwardedFor).orEmpty()
        .flatMap { it.split(',') }
        .map(String::trim)
        .filter(String::isNotEmpty)
    return forwarded.getOrNull(forwarded.size - trustedProxies) ?: peer
}
