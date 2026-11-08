package app.katari.sync.client.identity

import okio.Buffer
import kotlin.uuid.Uuid

/**
 * Record keys, unique within an account and the same for one record on every device.
 *
 * A record that comes from a source, or hangs off one that does, has a key computed from stable values, so two
 * devices that add the same entry offline arrive at the same key with nothing to match up later. A record a person
 * made from nothing (a category, a profile other than `default`) has a random key, matched by name at first sync.
 *
 * Computed keys are a compatibility boundary: every device and every app version must compute the same key from the
 * same values, so neither [derive] nor the parts any kind passes to it may ever change. A kind that needs different
 * parts takes a new kind name, and with it new keys. [derive] hashes, with SHA-256:
 * - the derivation version, one byte, [DERIVATION_VERSION];
 * - each part as its UTF-8 length in 4 bytes, big-endian, then its UTF-8 bytes. The first part is the kind name, so
 *   two kinds never share a key.
 *
 * The key is the first 16 bytes of the hash, which is as wide as a UUID, in unpadded base64url (22 characters).
 * Random keys have the same shape.
 */
object RecordKeys {
    const val DERIVATION_VERSION: Byte = 1
    private const val KEY_BYTES = 16L

    fun derive(kind: String, vararg parts: String): String {
        val tuple = Buffer().writeByte(DERIVATION_VERSION.toInt())
        (listOf(kind) + parts).forEach { part ->
            val bytes = part.encodeToByteArray()
            tuple.writeInt(bytes.size).write(bytes)
        }
        return tuple.readByteString().sha256().substring(0, KEY_BYTES.toInt()).base64Url().trimEnd('=')
    }

    fun random(): String = Buffer().write(Uuid.random().toByteArray()).readByteString().base64Url().trimEnd('=')
}
