package app.katari.sync.client.message

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.message.MessageBody
import app.katari.sync.protocol.message.SendMessageRequest
import dev.zacsweers.metro.Inject
import tachiyomi.data.DatabaseHandler
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Messages to the account's other devices, kept here until the server takes them, so one sent without a connection
 * goes once there is one. A message still waiting when it expires is dropped, and so is one still waiting when the
 * same [SendMessageRequest.replaces] is sent to the same devices again.
 */
@Inject
class MessageOutbox(private val handler: DatabaseHandler) {
    internal suspend fun add(to: DeviceId?, body: MessageBody, lifetime: Duration, replaces: String) {
        val expiresAt = nowMillis() + lifetime.inWholeMilliseconds
        handler.await(inTransaction = true) {
            if (replaces.isNotEmpty()) sync_message_outboxQueries.deleteReplaced(replaces, to?.value)
            sync_message_outboxQueries.insert(
                target = to?.value,
                kind = body.kind,
                kindVersion = body.kindVersion.toLong(),
                payload = body.payload,
                replaces = replaces,
                expiresAt = expiresAt,
            )
        }
    }

    internal suspend fun count(): Long = handler.await { sync_message_outboxQueries.count().awaitAsOne() }

    /**
     * Sends every message waiting, oldest first, each removed once the server took it. One to a device that left the
     * account is dropped. Runs deliver, one at a time, so no message goes twice.
     */
    internal suspend fun deliver(client: SyncServerClient) {
        while (true) {
            val now = nowMillis()
            val batch = handler.await(inTransaction = true) {
                sync_message_outboxQueries.deleteExpired(now)
                sync_message_outboxQueries.oldest(BATCH).awaitAsList()
            }
            if (batch.isEmpty()) return
            batch.forEach { row ->
                client.sendMessage(
                    SendMessageRequest(
                        to = row.target_device_id?.let(::DeviceId),
                        body = MessageBody(row.kind, row.kind_version.toInt(), row.payload),
                        // What is left of its lifetime: the server holds it as long as it would have waited here
                        lifetimeMillis = (row.expires_at - now).coerceAtLeast(1),
                        replaces = row.replaces,
                    ),
                )
                handler.await { sync_message_outboxQueries.delete(row.seq) }
            }
        }
    }

    private fun nowMillis() = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val BATCH = 50L
    }
}
