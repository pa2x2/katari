package app.katari.sync.client.downloadrequest

import app.katari.sync.client.identity.SyncedEntries
import app.katari.sync.client.message.DeviceMessaging
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.message.MessageLimits
import dev.zacsweers.metro.Inject

/** A chapter of the entry [entryId] to ask another device to download. */
data class ChapterToRequest(val entryId: Long, val url: String, val name: String)

/**
 * Asks another device of the account to download chapters. A device can only be asked about an entry it can have,
 * one that syncs ([SyncedEntries]); the request waits for it on the server as long as messages may.
 */
@Inject
class DownloadRequestSending(
    private val messaging: DeviceMessaging,
    private val entries: SyncedEntries,
) {
    /** The entries among [entryIds] another device can be asked to download chapters of. */
    suspend fun requestable(entryIds: Collection<Long>): Set<Long> =
        entryIds.toSet().filterTo(mutableSetOf()) { entries.keyOf(it) != null }

    /**
     * Asks [to] to download [chapters], one request per entry, leaving out the chapters of entries that don't sync.
     * Returns how many chapters were asked for.
     */
    suspend fun send(to: DeviceId, chapters: List<ChapterToRequest>): Int {
        var sent = 0
        chapters.groupBy { it.entryId }.forEach { (entryId, ofEntry) ->
            val key = entries.keyOf(entryId) ?: return@forEach
            val requested = ofEntry.map { RequestedChapter(it.url, it.name) }
            DownloadRequestMessage.bodies(key, requested).forEach { body ->
                messaging.send(to, body, lifetime = MessageLimits.MAX_LIFETIME)
            }
            sent += ofEntry.size
        }
        return sent
    }
}
