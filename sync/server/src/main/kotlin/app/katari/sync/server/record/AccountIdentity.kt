package app.katari.sync.server.record

import app.katari.sync.protocol.server.ServerIdentity
import app.katari.sync.server.storage.ServerStorage

internal fun ServerStorage.identityOf(accountId: Long): ServerIdentity {
    val sequence = records.sequence(accountId)
    return ServerIdentity(
        instanceId = instanceId,
        sequence = sequence,
        // Retention keeps the chain from the oldest position a device holds, which is never past the newest sequence.
        chain = checkNotNull(chainAt(accountId, sequence)).toHexString(),
    )
}

/** Null when [sequence] is before the history kept. */
internal fun ServerStorage.chainAt(accountId: Long, sequence: Long): ByteArray? =
    if (sequence == 0L) HistoryChain.genesis(instanceId) else records.chainAt(accountId, sequence)
