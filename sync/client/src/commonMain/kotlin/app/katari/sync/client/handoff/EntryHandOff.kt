package app.katari.sync.client.handoff

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.account.DeviceDirectory
import app.katari.sync.client.apply.IncomingRecords
import app.katari.sync.client.kind.progress.ProgressKind
import app.katari.sync.client.merge.newestEdit
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordGroups
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.client.setting.read.SettingReads
import app.katari.sync.protocol.clock.HybridTimestamp
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.record.SequencedRecord
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import tachiyomi.data.DatabaseHandler

/**
 * Picking up where another device left off (D052, D053). A run fetches the entries it was asked about ahead of
 * everything else ([fetchFirst]), by their record group, and every record a run applies is watched for progress
 * another device made that is newer than what this device held: [arrivals] reports it, and [checks] says for each
 * entry asked about whether the check is still running, found something, or couldn't reach the server.
 */
@Inject
@SingleIn(AppScope::class)
class EntryHandOff(
    private val handler: DatabaseHandler,
    private val incoming: IncomingRecords,
    private val settings: SettingReads,
    private val devices: DeviceDirectory,
) {
    private val checkStates = MutableStateFlow<Map<Long, HandOffCheck>>(emptyMap())
    private val arrived = MutableSharedFlow<ProgressArrival>(
        extraBufferCapacity = ARRIVALS_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** By entry ID, for the entries checks were asked for since the process started. */
    val checks: StateFlow<Map<Long, HandOffCheck>> = checkStates.asStateFlow()

    val arrivals: SharedFlow<ProgressArrival> = arrived.asSharedFlow()

    internal fun checking(entryIds: Collection<Long>) {
        if (entryIds.isEmpty()) return
        checkStates.update { states -> states + entryIds.associateWith { HandOffCheck.Checking } }
    }

    internal fun unreachable(entryIds: Collection<Long>) {
        if (entryIds.isEmpty()) return
        checkStates.update { states -> states + entryIds.associateWith { HandOffCheck.Unreachable } }
    }

    /** Fetches and applies the records of [entryIds] ahead of the device's position. */
    internal suspend fun fetchFirst(client: SyncServerClient, device: DeviceId, entryIds: Collection<Long>) {
        entryIds.forEach { entryId ->
            val key = handler.await { LocalReads(this, device, settings).entryKey(entryId) }
            if (key == null) {
                // Not an entry that syncs, such as one of a local source: there is nothing elsewhere to wait for.
                checkStates.update { it + (entryId to HandOffCheck.Checked(null)) }
                return@forEach
            }
            val records = client.group(RecordGroups.entry(key)).records
            val found = watching(client, device, records) { incoming.applyAhead(records) }
            checkStates.update { it + (entryId to HandOffCheck.Checked(found.firstOrNull { it.entryId == entryId })) }
        }
    }

    /**
     * Runs [apply], which applies [records], and reports the progress among them that another device made after
     * anything this device held of it. Returns what it reported.
     */
    internal suspend fun watching(
        client: SyncServerClient,
        device: DeviceId,
        records: List<SequencedRecord>,
        apply: suspend () -> Unit,
    ): List<ProgressArrival> {
        val candidates = records.mapNotNull { candidate(it, device) }
        apply()
        if (candidates.isEmpty()) return emptyList()
        val arrivals = candidates.groupBy { it.entryKey }.mapNotNull { (entryKey, edits) ->
            val newest = edits.maxBy { it.at }
            val entryId = entryIdOf(entryKey) ?: return@mapNotNull null
            ProgressArrival(
                entryId = entryId,
                device = newest.at.device,
                deviceName = devices.nameOf(newest.at.device, client),
                editedAt = newest.at.physicalMillis,
            )
        }
        arrivals.forEach { arrived.tryEmit(it) }
        return arrivals
    }

    /** A progress record another device edited after this device's copy, read before the record is applied. */
    private suspend fun candidate(record: SequencedRecord, device: DeviceId): Candidate? {
        val envelope = record.record
        if (envelope.kind != ProgressKind.name || envelope.kindVersion != ProgressKind.version || envelope.deleted) {
            return null
        }
        val payload = ProgressKind.decode(envelope.payload)
        val edited = newestEdit(ProgressKind.serializer, payload) ?: return null
        if (edited.device == device) return null
        val stored = handler.await { sync_recordsQueries.getByKey(envelope.key).awaitAsOneOrNull() }
            ?.takeUnless { it.deleted }
            ?.let { newestEdit(ProgressKind.serializer, ProgressKind.decode(it.payload)) }
        if (stored != null && stored >= edited) return null
        return Candidate(payload.entryKey, edited)
    }

    private suspend fun entryIdOf(entryKey: String): Long? = handler.await {
        sync_recordsQueries.getByKey(entryKey).awaitAsOneOrNull()?.local_ref?.let { LocalRef(it).long(0) }
    }

    internal fun forget() {
        checkStates.value = emptyMap()
        devices.forget()
    }

    private class Candidate(val entryKey: String, val at: HybridTimestamp)

    private companion object {
        const val ARRIVALS_BUFFER = 16
    }
}
