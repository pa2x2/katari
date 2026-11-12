package app.katari.sync.server.message

import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.message.DeviceMessage
import app.katari.sync.protocol.message.MessagePage
import app.katari.sync.protocol.message.MessageRecipient
import app.katari.sync.protocol.message.SendMessageRequest
import app.katari.sync.protocol.message.SendMessageResponse
import app.katari.sync.server.device.Device
import app.katari.sync.server.live.AccountChanges
import app.katari.sync.server.live.ConnectedDevices
import app.katari.sync.server.storage.ServerStorage
import kotlin.time.Clock

/**
 * Messages between an account's devices. Sending queues the message for each target and announces it on the target's
 * live channel; a device fetches what waits for it, which also confirms what it handled. A device holds at most
 * [MAX_WAITING]: one more drops its oldest, since a device that let that many pile up has been away from what they
 * asked the longest, and the sender is never held up by a device that doesn't fetch.
 */
class DeviceMessages(
    private val storage: ServerStorage,
    private val changes: AccountChanges,
    private val connected: ConnectedDevices,
    private val clock: Clock,
) {
    /** Null when [SendMessageRequest.to] names no other device of the sender's account that is still linked. */
    fun send(sender: Device, request: SendMessageRequest): SendMessageResponse? {
        val now = clock.now().toEpochMilliseconds()
        val queued = storage.transaction {
            targetsOf(sender, request.to)?.map { target ->
                if (request.replaces.isNotEmpty()) storage.messages.deleteReplaced(target, sender.id, request.replaces)
                val overflow = storage.messages.count(target) - (MAX_WAITING - 1)
                if (overflow > 0) storage.messages.deleteOldest(target, overflow)
                target to storage.messages.insert(
                    target = target,
                    from = sender.id,
                    body = request.body,
                    replaces = request.replaces,
                    sentAtMillis = now,
                    expiresAtMillis = now + request.lifetimeMillis,
                )
            }
        } ?: return null
        queued.forEach { (target, number) -> changes.announceMessage(target, number) }
        return SendMessageResponse(
            queued.map { (target, _) ->
                MessageRecipient(target, connected.isConnected(target))
            },
        )
    }

    /** Confirms [device]'s messages up to [after] and answers with those after it. */
    fun fetch(device: DeviceId, after: Long): MessagePage {
        val now = clock.now().toEpochMilliseconds()
        val waiting = storage.transaction {
            storage.messages.confirm(device, after)
            storage.messages.waitingAfter(device, after, now, limit = PAGE_LIMIT + 1L)
        }
        val page = mutableListOf<DeviceMessage>()
        var bytes = 0L
        for (message in waiting.take(PAGE_LIMIT)) {
            bytes += message.body.payload.size
            if (page.isNotEmpty() && bytes > PAGE_MAX_BYTES) break
            page += message
        }
        return MessagePage(messages = page, hasMore = page.size < waiting.size)
    }

    /**
     * What [device]'s live channel tells of its messages: the newest number, and every waiting message when together
     * they are small enough to go along.
     */
    fun waitingFor(device: DeviceId): WaitingMessages {
        val now = clock.now().toEpochMilliseconds()
        return storage.transaction {
            val totals = storage.messages.waitingTotals(device, now)
            val fitsAlong = totals.count in 1..INLINE_MAX_COUNT && totals.payloadBytes <= INLINE_MAX_BYTES
            WaitingMessages(
                newest = totals.newest,
                messages = if (fitsAlong) storage.messages.waitingAfter(device, 0, now, totals.count) else emptyList(),
            )
        }
    }

    fun forgetExpired() {
        storage.transaction {
            storage.messages.deleteExpired(clock.now().toEpochMilliseconds())
            storage.messages.deleteForRevokedDevices()
        }
    }

    private fun targetsOf(sender: Device, to: DeviceId?): List<DeviceId>? {
        if (to == null) return storage.devices.activeIn(sender.accountId).map { it.id }.filter { it != sender.id }
        val target = storage.devices.byId(to)
            ?.takeIf { it.accountId == sender.accountId && !it.isRevoked && it.id != sender.id }
        return target?.let { listOf(it.id) }
    }

    companion object {
        const val MAX_WAITING = 1_000L
        private const val PAGE_LIMIT = 100

        // A page holds at least one message, so one at the payload limit still goes.
        private const val PAGE_MAX_BYTES = 1024L * 1024

        // Small enough that a channel announcing them costs about what one fetch would.
        private const val INLINE_MAX_COUNT = 32L
        private const val INLINE_MAX_BYTES = 16L * 1024
    }
}

data class WaitingMessages(
    /** 0 when none waits. */
    val newest: Long,
    /** Empty when none waits or they are too many or too large to go along. */
    val messages: List<DeviceMessage>,
)
