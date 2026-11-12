package app.katari.sync.server.message

import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.message.DeviceMessage
import app.katari.sync.protocol.message.MessageBody

interface MessageStore {
    /** The number the message was given. */
    fun insert(
        target: DeviceId,
        from: DeviceId,
        body: MessageBody,
        replaces: String,
        sentAtMillis: Long,
        expiresAtMillis: Long,
    ): Long

    fun deleteReplaced(target: DeviceId, from: DeviceId, replaces: String)

    /** Expired messages count too, until they are swept. */
    fun count(target: DeviceId): Long

    fun deleteOldest(target: DeviceId, count: Long)

    fun waitingAfter(target: DeviceId, after: Long, nowMillis: Long, limit: Long): List<DeviceMessage>

    fun waitingTotals(target: DeviceId, nowMillis: Long): WaitingTotals

    /** Drops [target]'s messages up to [through], which it handled. */
    fun confirm(target: DeviceId, through: Long)

    fun deleteExpired(nowMillis: Long)

    fun deleteForRevokedDevices()
}

data class WaitingTotals(
    val count: Long,
    val payloadBytes: Long,
    /** The highest number among them, 0 when none waits. */
    val newest: Long,
)
