package app.katari.sync.server.storage.sqlite

import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.message.DeviceMessage
import app.katari.sync.protocol.message.MessageBody
import app.katari.sync.server.message.MessageStore
import app.katari.sync.server.message.WaitingTotals

internal class SqliteMessageStore(database: SyncServerDatabase) : MessageStore {
    private val queries = database.device_messagesQueries
    private val numbers = database.message_numbersQueries

    override fun insert(
        target: DeviceId,
        from: DeviceId,
        body: MessageBody,
        replaces: String,
        sentAtMillis: Long,
        expiresAtMillis: Long,
    ): Long {
        numbers.give(sentAtMillis)
        val number = numbers.lastGiven().executeAsOne()
        queries.insert(
            number = number,
            target = target.value,
            from = from.value,
            kind = body.kind,
            kindVersion = body.kindVersion.toLong(),
            payload = body.payload,
            replaces = replaces,
            sentAt = sentAtMillis,
            expiresAt = expiresAtMillis,
        )
        return number
    }

    override fun deleteReplaced(target: DeviceId, from: DeviceId, replaces: String) {
        queries.deleteReplaced(target = target.value, from = from.value, replaces = replaces)
    }

    override fun count(target: DeviceId): Long = queries.count(target.value).executeAsOne()

    override fun deleteOldest(target: DeviceId, count: Long) {
        queries.deleteOldest(target = target.value, count = count)
    }

    override fun waitingAfter(target: DeviceId, after: Long, nowMillis: Long, limit: Long): List<DeviceMessage> =
        queries.waitingAfter(target = target.value, after = after, now = nowMillis, limit = limit)
            .executeAsList()
            .map { it.toMessage() }

    override fun waitingTotals(target: DeviceId, nowMillis: Long): WaitingTotals =
        queries.totalsWaiting(target = target.value, now = nowMillis).executeAsOne().let {
            // SQL's sum and max of no rows are null
            WaitingTotals(count = it.count, payloadBytes = it.bytes ?: 0, newest = it.newest ?: 0)
        }

    override fun confirm(target: DeviceId, through: Long) {
        queries.confirm(target = target.value, through = through)
    }

    override fun deleteExpired(nowMillis: Long) {
        queries.deleteExpired(nowMillis)
    }

    override fun deleteForRevokedDevices() {
        queries.deleteForRevokedDevices()
    }

    private fun Device_messages.toMessage() = DeviceMessage(
        number = _id,
        from = DeviceId(from_device_id),
        body = MessageBody(kind = kind, kindVersion = kind_version.toInt(), payload = payload),
        sentAtMillis = sent_at,
        expiresAtMillis = expires_at,
    )
}
