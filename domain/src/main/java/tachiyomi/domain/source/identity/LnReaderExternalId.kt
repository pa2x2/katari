package tachiyomi.domain.source.identity

import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * The external ID of the LNReader plugin whose own ID is [pluginId] (case-sensitive text such as `novelupdates` or
 * `FWN.com`): the first 8 bytes of the SHA-256 of its UTF-8 bytes, big-endian, sign bit cleared. It names the plugin's
 * settings files and its identity, so it must never change.
 */
fun lnReaderExternalId(pluginId: String): Long {
    val digest = MessageDigest.getInstance("SHA-256").digest(pluginId.toByteArray(Charsets.UTF_8))
    return ByteBuffer.wrap(digest, 0, Long.SIZE_BYTES).long and Long.MAX_VALUE
}
