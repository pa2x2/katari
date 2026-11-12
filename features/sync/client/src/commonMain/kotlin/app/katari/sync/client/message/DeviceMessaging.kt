package app.katari.sync.client.message

import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.run.SyncRequest
import app.katari.sync.client.run.SyncRuns
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.message.MessageBody
import app.katari.sync.protocol.message.MessageLimits
import dev.zacsweers.metro.Inject
import kotlin.time.Duration

/**
 * Sends messages to the account's other devices ([app.katari.sync.protocol.message.DeviceMessage]), where the
 * [MessageHandler] of their kind takes them. A message waits in the [MessageOutbox] until a run takes it to the
 * server, which sending asks for at once, so it goes as soon as syncing may: not while syncing is paused, nor on a
 * network changes don't use.
 */
@Inject
class DeviceMessaging(
    private val connection: SyncConnection,
    private val outbox: MessageOutbox,
    private val runs: SyncRuns,
) {
    /**
     * Sends [body] to [to], or to every other device of the account when null, and drops it if it hasn't reached the
     * server and a device fetching it within [lifetime]. Nothing goes while this device isn't linked to an account.
     */
    suspend fun send(to: DeviceId?, body: MessageBody, lifetime: Duration, replaces: String = "") {
        require(lifetime.isPositive() && lifetime <= MessageLimits.MAX_LIFETIME) { "Message lifetime $lifetime" }
        require(body.payload.size <= MessageLimits.MAX_PAYLOAD_BYTES) {
            "Message payload of ${body.payload.size} bytes"
        }
        if (!connection.state().isLinked) return
        outbox.add(to, body, lifetime, replaces)
        runs.request(SyncRequest(onlyIfChanged = true))
    }
}
