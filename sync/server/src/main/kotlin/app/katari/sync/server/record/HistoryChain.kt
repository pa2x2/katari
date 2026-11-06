package app.katari.sync.server.record

import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * The fingerprint of an account's history ([app.katari.sync.protocol.server.ServerIdentity.chain]). The value at a
 * sequence is the SHA-256 of the value before it, the sequence, and the record key and change id written there, so it
 * differs between two servers as soon as one gave any sequence up to it to another change. That is how a device tells
 * a server restored from an old copy, which has since given the device's sequences to other pushes, from the one it
 * synced with. Before the first write it is derived from the instance id, so servers that started over never agree.
 */
internal object HistoryChain {
    fun genesis(instanceId: String): ByteArray = sha256("katari-sync-history\u0000$instanceId".encodeToByteArray())

    fun next(previous: ByteArray, sequence: Long, key: String, changeId: String): ByteArray {
        val keyBytes = key.encodeToByteArray()
        val changeBytes = changeId.encodeToByteArray()
        // Lengths before contents, so no two different (key, change id) pairs hash the same bytes.
        val size = previous.size + Long.SIZE_BYTES + 2 * Int.SIZE_BYTES + keyBytes.size + changeBytes.size
        val input = ByteBuffer.allocate(size)
            .put(previous)
            .putLong(sequence)
            .putInt(keyBytes.size)
            .put(keyBytes)
            .putInt(changeBytes.size)
            .put(changeBytes)
        return sha256(input.array())
    }

    /**
     * The link that starts an account's history over after its records were wiped. No record has an empty key, so it
     * never equals the link of a write.
     */
    fun restart(previous: ByteArray, sequence: Long): ByteArray =
        next(previous, sequence, key = "", changeId = "history restarted")

    private fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
}
