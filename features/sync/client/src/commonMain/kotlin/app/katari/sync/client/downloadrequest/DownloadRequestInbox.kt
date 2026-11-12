package app.katari.sync.client.downloadrequest

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.katari.sync.client.account.DeviceDirectory
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.identity.SyncedEntries
import app.katari.sync.client.message.MessageHandler
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.message.DeviceMessage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.DatabaseHandler
import kotlin.time.Clock

/**
 * Takes the download requests the account's other devices send here and keeps each until its entry is here and the
 * [DownloadRequestPerformer] carried it out, or it expires. Messages come before the records of a run, so a request
 * about an entry added on the other device a moment ago comes before its entry does; every run that synced looks again
 * ([DownloadRequestRetry]). Requests are carried out apart from the run that brought them, one at a time, oldest first.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class)
class DownloadRequestInbox(
    private val handler: DatabaseHandler,
    private val connection: SyncConnection,
    private val entries: SyncedEntries,
    private val directory: DeviceDirectory,
    private val performer: DownloadRequestPerformer,
) : MessageHandler {
    override val kind: String = DownloadRequestMessage.KIND

    override val version: Int = DownloadRequestMessage.VERSION

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val working = Mutex()

    override suspend fun handle(message: DeviceMessage) {
        val payload = DownloadRequestMessage.decode(message.body.payload)
        handler.await {
            sync_download_requestsQueries.insert(
                fromDeviceId = message.from.value,
                entryKey = payload.entryKey,
                payload = message.body.payload,
                expiresAt = message.expiresAtMillis,
            )
        }
        lookAgain()
    }

    /** Carries out the requests waiting whose entry is here now, apart from whoever asked. */
    fun lookAgain(): Job = scope.launch { working.withLock { carryOut() } }

    private suspend fun carryOut() {
        if (!connection.state().isLinked) {
            // A request from an account this device left has no one to answer to
            handler.await { sync_download_requestsQueries.clear() }
            return
        }
        val waiting = handler.await {
            sync_download_requestsQueries.deleteExpired(Clock.System.now().toEpochMilliseconds())
            sync_download_requestsQueries.all().awaitAsList()
        }
        waiting.forEach { row ->
            val entryId = entries.entryOf(row.entry_key) ?: return@forEach
            val request = ReceivedDownloadRequest(
                entryId = entryId,
                chapters = DownloadRequestMessage.decode(row.payload).chapters,
                fromName = directory.listedName(DeviceId(row.from_device_id)),
            )
            val result = try {
                performer.perform(request)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // One that fails would fail again on every look; the requests after it still go
                logcat(LogPriority.ERROR, e) { "A download request wasn't carried out" }
                DownloadRequestResult.DONE
            }
            if (result == DownloadRequestResult.DONE) handler.await { sync_download_requestsQueries.delete(row._id) }
        }
    }
}
