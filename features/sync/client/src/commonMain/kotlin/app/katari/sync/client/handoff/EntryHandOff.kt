package app.katari.sync.client.handoff

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.account.DeviceDirectory
import app.katari.sync.client.apply.IncomingRecords
import app.katari.sync.client.kind.progress.ProgressKind
import app.katari.sync.client.merge.newestEdit
import app.katari.sync.client.payload.Stamped
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.data.DatabaseHandler
import kotlin.time.Duration.Companion.seconds

/**
 * Picking up where another device left off (D052, D053). A run fetches the entries it was asked about ahead of
 * everything else ([fetchFirst]), by their record group, and every record a run applies is watched for progress
 * another device made that is newer than what this device held: [arrivals] reports it, and [checks] says for each
 * entry asked about whether the check is still running, found something, or couldn't reach the server.
 *
 * Resume works at once (D052), so a check is given [CHECK_LIMIT]: one still running then, behind a run that is slow
 * or a server that never answers, is told as unreachable, and turns into what the fetch finds if it gets there later.
 * The run itself keeps its ordinary timeouts.
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Each entry's current check, so a limit set for an earlier one doesn't end a check started again since
    private val checkIds = HashMap<Long, Long>()
    private var lastCheckId = 0L
    private val arrived = MutableSharedFlow<ProgressArrival>(
        extraBufferCapacity = ARRIVALS_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** By entry ID, for the entries checks were asked for since the process started. */
    val checks: StateFlow<Map<Long, HandOffCheck>> = checkStates.asStateFlow()

    val arrivals: SharedFlow<ProgressArrival> = arrived.asSharedFlow()

    /** Whether a check was told unreachable and no run has got through since ([pulled]). */
    val toldUnreachable: Boolean
        get() = HandOffCheck.Unreachable in checkStates.value.values

    /** Starts checking [entryIds]; one already being checked carries on with the limit it has. */
    internal fun checking(entryIds: Collection<Long>) {
        val started = synchronized(checkIds) {
            val starting = entryIds.filter { checkStates.value[it] != HandOffCheck.Checking }
            if (starting.isEmpty()) return
            checkStates.update { states -> states + starting.associateWith { HandOffCheck.Checking } }
            starting.associateWith { ++lastCheckId }.also(checkIds::putAll)
        }
        scope.launch {
            delay(CHECK_LIMIT)
            resolveRunning(started.keys, HandOffCheck.Unreachable) { entryId -> checkIds[entryId] == started[entryId] }
        }
    }

    /**
     * Ends the checks of [entryIds] that the run asked to fetch them left running, as when it had nothing to fetch
     * them from or stopped on an error: [reached] when the run spoke with the server or had no reason to, so there was
     * nothing newer, and otherwise unreachable.
     */
    internal fun settle(entryIds: Collection<Long>, reached: Boolean) {
        resolveRunning(entryIds, if (reached) HandOffCheck.Checked(null) else HandOffCheck.Unreachable) { true }
    }

    private fun resolveRunning(entryIds: Collection<Long>, result: HandOffCheck, current: (Long) -> Boolean) {
        synchronized(checkIds) {
            checkStates.update { states ->
                states + entryIds.filter { states[it] == HandOffCheck.Checking && current(it) }.associateWith { result }
            }
        }
    }

    /**
     * After a run that pulled everything the server had: checks told unreachable before have nothing newer waiting
     * any more, as newer progress that pull brought came as [arrivals], so they stop saying the progress is this
     * device's own.
     */
    internal fun pulled() {
        synchronized(checkIds) {
            checkStates.update { states ->
                states.mapValues { (_, check) ->
                    if (check == HandOffCheck.Unreachable) HandOffCheck.Checked(null) else check
                }
            }
        }
    }

    /** Ends the running checks of [entryIds] as unreachable; one that already found something keeps it. */
    internal fun unreachable(entryIds: Collection<Long>) {
        resolveRunning(entryIds, HandOffCheck.Unreachable) { true }
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
        val candidates = candidates(records, device)
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

    /**
     * The progress records among [records] another device edited after this device's copies, read before the records
     * are applied, with this device's copies looked up together.
     */
    private suspend fun candidates(records: List<SequencedRecord>, device: DeviceId): List<Candidate> {
        val edits = records.mapNotNull { record -> otherDevicesEdit(record, device) }
        if (edits.isEmpty()) return emptyList()
        val stored = handler.await {
            edits.map { it.key }.chunked(LOOKUP_CHUNK).flatMap { keys ->
                sync_recordsQueries.livePayloads(keys).awaitAsList()
            }
        }.associate { it.record_key to newestEdit(ProgressKind.serializer, ProgressKind.decode(it.payload)) }
        return edits.filter { edit -> stored[edit.key].let { it == null || it < edit.candidate.at } }
            .map { it.candidate }
    }

    /** The edit [record] brings when it is a position another device changed. */
    private fun otherDevicesEdit(record: SequencedRecord, device: DeviceId): IncomingEdit? {
        val envelope = record.record
        if (envelope.kind != ProgressKind.name || envelope.kindVersion != ProgressKind.version || envelope.deleted) {
            return null
        }
        val payload = ProgressKind.decode(envelope.payload)
        val edited = newestEdit(ProgressKind.serializer, payload) ?: return null
        // A position recorded when its device set up sync carries no edit time: no device made it, and naming its
        // placeholder device would ask the server for a device that is never listed.
        if (edited == Stamped.UNKNOWN_EDIT || edited.device == device) return null
        return IncomingEdit(envelope.key, Candidate(payload.entryKey, edited))
    }

    private suspend fun entryIdOf(entryKey: String): Long? = handler.await {
        sync_recordsQueries.getByKey(entryKey).awaitAsOneOrNull()?.local_ref?.let { LocalRef(it).long(0) }
    }

    internal fun forget() {
        synchronized(checkIds) { checkIds.clear() }
        checkStates.value = emptyMap()
        devices.forget()
    }

    private class Candidate(val entryKey: String, val at: HybridTimestamp)

    private class IncomingEdit(val key: String, val candidate: Candidate)

    internal companion object {
        private const val ARRIVALS_BUFFER = 16

        // Keys per lookup, well under SQLite's limit on bound values in one statement
        private const val LOOKUP_CHUNK = 500

        val CHECK_LIMIT = 4.seconds
    }
}
