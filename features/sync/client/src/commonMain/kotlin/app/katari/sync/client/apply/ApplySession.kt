package app.katari.sync.client.apply

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.arrival.ArrivalNotes
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.SyncKinds
import app.katari.sync.client.kind.activity.ActivitySessionKind
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.profile.LocalProfileDeletion
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.kind.question.QuestionDetection
import app.katari.sync.client.kind.replacement.EntryReplacementKind
import app.katari.sync.client.kind.replacement.Replacement
import app.katari.sync.client.kind.replacement.carry.MigrationCarryRules
import app.katari.sync.client.kind.replacement.carry.ReplacedEntryEdits
import app.katari.sync.client.merge.RecordState
import app.katari.sync.client.merge.merge
import app.katari.sync.client.merge.newestEdit
import app.katari.sync.client.outbox.EditStamper
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.part.OffParts
import app.katari.sync.client.payload.SourceReference
import app.katari.sync.client.readelsewhere.ChaptersReadElsewhere
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordGroups
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.client.record.newChangeId
import app.katari.sync.client.setting.apply.SettingWrites
import app.katari.sync.client.setting.read.SettingReads
import app.katari.sync.client.setting.scope.SettingsArea
import app.katari.sync.data.Sync_pending
import app.katari.sync.protocol.clock.HybridLogicalClock
import app.katari.sync.protocol.clock.HybridTimestamp
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.record.RecordEnvelope
import app.katari.sync.protocol.record.SequencedRecord
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.protobuf.ProtoBuf
import tachiyomi.data.Database
import tachiyomi.domain.source.identity.SourceIdScheme

/**
 * Applies pulled records inside one transaction: merges each with the copy this device stores (which holds its own
 * edits not pushed yet), writes the result to the local rows, and stores it as the record's new copy.
 *
 * A record is stored as merged even when its rows can't be written yet, so the device's copy always holds every edit
 * it has seen. Its rows wait in `sync_pending` for what they need: the record they refer to ([HoldReason.PARENT],
 * retried as soon as that record is applied), or a reason that passes by itself, retried with each sync. A record of
 * a kind or kind version this app can't read is held as received and not merged at all, and while it waits this
 * device's own changes to that record are not pushed, so its older format never replaces the newer one.
 *
 * The merged copy is pushed back only when it holds edits the incoming copy lacks; a record this device had no
 * unpushed edits of is stored exactly as received and produces nothing to push.
 *
 * Applying a record that throws fails as [RecordApplyFailure], naming that record even when it was written on behalf of
 * another, as a parent is when its child resolves it. The transaction then runs again with the record in [failing],
 * which holds it aside ([HoldReason.FAILED]) as far as the failure got, and every other record applies (D067).
 *
 * A record of a part of the account this device chose not to sync ([OffParts]) is held as received
 * ([HoldReason.TURNED_OFF]) and not merged: its local row isn't written, and merging into the stored copy alone would
 * pair the row's values with the account's edit times the next time the row is drained. [followChoices] takes it in
 * once the part is synced again.
 */
internal class ApplySession(
    override val database: Database,
    override val settings: SettingWrites,
    override val profileDeletion: LocalProfileDeletion,
    override val migrationRules: MigrationCarryRules,
    override val activeProfileId: Long,
    private val parts: OffParts,
    private val device: DeviceId,
    private val appVersion: String,
    private val failing: Map<String, FailedStage>,
    private val settingReads: SettingReads,
    /** Where a change from another device is noted; null in a session that applies no arrivals, such as a choice's. */
    private val arrivals: ArrivalNotes?,
    private val now: () -> Long,
) : LocalWrites {
    private val resolving = HashSet<String>()
    private val questions = QuestionDetection(this)
    private val movedEdits = MovedRecordEdits(this, parts)
    private val afterRecords = LinkedHashMap<String, AfterRecords>()
    private val readHere = LinkedHashMap<Long, LinkedHashSet<Long>>()
    private val markedHere = LinkedHashMap<Long, LinkedHashSet<Long>>()

    /** The newest edit taken in, which this device's later edits must be stamped after. */
    var newestSeen: HybridTimestamp? = null
        private set

    suspend fun receive(record: SequencedRecord) {
        val envelope = record.record
        if (failing[envelope.key] == FailedStage.STORING) return hold(record, HoldReason.FAILED)
        val kind = SyncKinds.named(envelope.kind)
        if (kind == null || !kind.reads(envelope.kindVersion)) return hold(record, HoldReason.UNREADABLE)
        receive(kind, record)
    }

    /**
     * Applies again what waits for a reason that may have passed: an update, the clock, a reference, a profile. A
     * record whose copy here already holds what was received is written from that copy, which keeps the edits it has
     * waiting to be pushed and the server copy it is based on: a record held after it was merged, or one this device
     * pushed an edit of since, would otherwise be based on the older copy held again, and its next push refused as
     * stale. A newer one received is merged first.
     */
    suspend fun retryHeld() {
        database.sync_pendingQueries.retryable(appVersion).awaitAsList().forEach { held -> retry(held) }
    }

    /**
     * Tries [held] again: writes the stored copy it was held after storing, or receives it whole when it was held
     * before this device stored it, as a deletion waiting for the record its edits move to ([MovedRecordEdits]).
     */
    private suspend fun retry(held: Sync_pending) {
        database.sync_pendingQueries.release(held.record_key)
        val record = ProtoBuf.decodeFromByteArray(SequencedRecord.serializer(), held.record)
        val stored = database.sync_recordsQueries.getByKey(held.record_key).awaitAsOneOrNull()
        if (stored != null && stored.based_on >= record.sequence) {
            reapply(held.record_key, held)
        } else {
            receive(record)
        }
    }

    private suspend fun <P : Any> receive(kind: SyncedKind<P>, record: SequencedRecord) {
        val envelope = record.record
        var stage = FailedStage.STORING
        try {
            if (parts.isOff(kind, envelope.deleted, envelope.payload)) return hold(record, HoldReason.TURNED_OFF)
            val incoming = RecordState.of(envelope.deleted, envelope.payload)
            val edited = kind.newestEdit(incoming)
            if (edited != null && edited.physicalMillis - now() > HybridLogicalClock.MAX_AHEAD.inWholeMilliseconds) {
                return hold(record, HoldReason.AHEAD)
            }
            edited?.let(::see)
            database.sync_pendingQueries.release(envelope.key)

            val earlier = database.sync_recordsQueries.getByKey(envelope.key).awaitAsOneOrNull()
            val earlierState = earlier?.let { RecordState.of(it.deleted, it.payload) }
            movedEdits.waitsFor(kind, earlierState, incoming)?.let { replacement ->
                return hold(record, HoldReason.PARENT, waitsFor = replacement)
            }
            questions.detect(kind, envelope.key, earlier, incoming)
            val stored = database.sync_recordsQueries.getByKey(envelope.key).awaitAsOneOrNull()
            val local = stored?.let { RecordState.of(it.deleted, it.payload) }
            val merged = movedEdits.settled(kind, local, incoming, local?.let { kind.merge(it, incoming) } ?: incoming)
            val holdsLocalEdits = !kind.sameRecord(merged, incoming)
            val payload = if (holdsLocalEdits) merged.bytes else envelope.payload
            database.sync_recordsQueries.storeMerged(
                recordKey = envelope.key,
                kind = kind.name,
                localRef = stored?.local_ref,
                kindVersion = kind.version.toLong(),
                payload = payload,
                deleted = merged.deleted,
                basedOn = record.sequence,
                changeId = if (holdsLocalEdits) newChangeId() else null,
                basePayload = envelope.payload,
                recordGroup = RecordGroups.of(kind.name, envelope.key, merged.deleted, payload),
            )
            stage = FailedStage.WRITING
            val outcome = if (envelope.key in failing) {
                ApplyOutcome.Held(HoldReason.FAILED)
            } else {
                write(kind, envelope.key, merged, stored?.local_ref)
            }
            settle(kind, envelope.key, outcome, stored?.local_ref, record)
            if (outcome is ApplyOutcome.Applied && merged is RecordState.Live) carry(kind, envelope.key, merged)
            movedEdits.carry(kind, local, incoming, merged)
        } catch (e: Exception) {
            throw failure(envelope.key, stage, e)
        }
    }

    private suspend fun <P : Any> carry(kind: SyncedKind<P>, key: String, state: RecordState.Live) {
        ReplacedEntryEdits(this).carry(kind, key, kind.decode(state.bytes))
    }

    override suspend fun reapply(key: String) {
        reapply(key, database.sync_pendingQueries.get(key).awaitAsOneOrNull())
    }

    /** Writes the record [key] as this device stores it again, once what it waited for is here. */
    private suspend fun reapply(key: String, held: Sync_pending?): ApplyOutcome? {
        val stored = database.sync_recordsQueries.getByKey(key).awaitAsOneOrNull() ?: return null
        val kind = SyncKinds.named(stored.kind) ?: return null
        try {
            val state = RecordState.of(stored.deleted, stored.payload)
            if (parts.isOff(kind, stored.deleted, stored.payload)) {
                held?.let {
                    hold(ProtoBuf.decodeFromByteArray(SequencedRecord.serializer(), it.record), HoldReason.TURNED_OFF)
                }
                return null
            }
            val outcome = if (key in failing) {
                ApplyOutcome.Held(HoldReason.FAILED)
            } else {
                write(kind, key, state, stored.local_ref)
            }
            val record = held?.let { ProtoBuf.decodeFromByteArray(SequencedRecord.serializer(), it.record) }
            settle(kind, key, outcome, stored.local_ref, record)
            if (outcome is ApplyOutcome.Applied && state is RecordState.Live) carry(kind, key, state)
            return outcome
        } catch (e: Exception) {
            throw failure(key, FailedStage.WRITING, e)
        }
    }

    override fun afterRecords(id: String, cause: String, action: suspend () -> Unit) {
        afterRecords.putIfAbsent(id, AfterRecords(cause, action))
    }

    override fun chapterRead(entryId: Long, chapterId: Long, marked: Boolean) {
        (if (marked) markedHere else readHere).getOrPut(entryId, ::LinkedHashSet) += chapterId
    }

    override suspend fun settingArriving(profileId: Long?, key: String, value: Any?, at: HybridTimestamp) {
        val notes = arrivals ?: return
        if (at.device == device) return
        val previous = when (profileId) {
            null -> settingReads.sharedValue(key)
            else -> settingReads.profileValue(profileId, key)
        }
        if (previous != value) notes.note(profileId, key, previous, at)
    }

    /** What [chapterRead] noted, by entry. */
    val chaptersRead: List<ChaptersReadElsewhere>
        get() = (readHere.keys + markedHere.keys).map { entryId ->
            ChaptersReadElsewhere(
                entryId = entryId,
                readIds = readHere[entryId].orEmpty(),
                markedIds = markedHere[entryId].orEmpty(),
            )
        }

    /** Runs the work records asked for [afterRecords], including what that work asks for in turn. */
    suspend fun finish() {
        while (afterRecords.isNotEmpty()) {
            val id = afterRecords.keys.first()
            val work = checkNotNull(afterRecords.remove(id))
            try {
                work.action()
            } catch (e: Exception) {
                throw failure(work.cause, FailedStage.WRITING, e)
            }
        }
    }

    private class AfterRecords(val cause: String, val action: suspend () -> Unit)

    /** [e] as the failure of the record [key], unless it already is a record's own, or a cancellation. */
    private fun failure(key: String, stage: FailedStage, e: Exception): Exception = when (e) {
        is RecordApplyFailure, is CancellationException -> e
        else -> RecordApplyFailure(key, stage, e)
    }

    private suspend fun settle(
        kind: SyncedKind<*>,
        key: String,
        outcome: ApplyOutcome,
        previousRef: String?,
        record: SequencedRecord?,
    ) {
        when (outcome) {
            is ApplyOutcome.Applied -> {
                outcome.ref?.let { movedEdits.takeOver(kind, key, it.json) }
                link(kind, key, outcome.ref?.json)
                database.sync_pendingQueries.release(key)
                retryWaitingFor(key)
            }
            is ApplyOutcome.Superseded -> {
                val tombstone = RecordState.Deleted(outcome.tombstone).bytes
                database.sync_recordsQueries.storeLocalChange(
                    recordKey = key,
                    kind = kind.name,
                    localRef = null,
                    kindVersion = kind.version.toLong(),
                    payload = tombstone,
                    deleted = true,
                    changeId = newChangeId(),
                    recordGroup = RecordGroups.of(kind.name, key, deleted = true, tombstone),
                )
                link(kind, key, null)
                database.sync_pendingQueries.release(key)
                retryWaitingFor(key)
            }
            is ApplyOutcome.WaitsFor -> {
                link(kind, key, previousRef)
                record?.let { hold(it, HoldReason.PARENT, waitsFor = outcome.key) }
            }
            is ApplyOutcome.Held -> {
                link(kind, key, previousRef)
                if (outcome.reason == HoldReason.FAILED) {
                    hold(record ?: storedCopy(key), HoldReason.FAILED)
                } else {
                    record?.let { hold(it, outcome.reason) }
                }
            }
        }
    }

    private suspend fun link(kind: SyncedKind<*>, key: String, ref: String?) {
        if (ref != null) database.sync_recordsQueries.detachLocalRef(kind.name, ref, key)
        database.sync_recordsQueries.setLocalRef(localRef = ref, recordKey = key)
    }

    private suspend fun retryWaitingFor(key: String) {
        database.sync_pendingQueries.waitingFor(key).awaitAsList().forEach { held -> retry(held) }
    }

    /**
     * The record [key] as this device stores it, for holding aside one that failed when written for another record:
     * as stored, at the server copy it is based on, so trying it again writes it from the store.
     */
    private suspend fun storedCopy(key: String): SequencedRecord {
        val stored = database.sync_recordsQueries.getByKey(key).awaitAsOne()
        return SequencedRecord(
            sequence = stored.based_on,
            record = RecordEnvelope(
                key = key,
                kind = stored.kind,
                kindVersion = stored.kind_version.toInt(),
                basedOn = stored.based_on,
                changeId = stored.change_id.orEmpty(),
                device = device,
                deleted = stored.deleted,
                payload = stored.payload,
            ),
        )
    }

    private suspend fun <P : Any> write(
        kind: SyncedKind<P>,
        key: String,
        state: RecordState,
        storedRef: String?,
    ): ApplyOutcome {
        val moved = ReplacedEntryEdits(this)
        return when (state) {
            is RecordState.Live -> {
                val payload = kind.decode(state.bytes)
                if (moved.movedAway(kind, payload)) ApplyOutcome.Applied(null) else kind.apply(this, key, payload)
            }
            is RecordState.Deleted -> {
                val last = state.tombstone.last.takeIf { it.isNotEmpty() }?.let(kind::decode)
                if (last != null && moved.movedAway(kind, last)) {
                    ApplyOutcome.Applied(null)
                } else {
                    kind.applyDeletion(this, key, storedRef?.let(::LocalRef))
                }
            }
        }
    }

    private suspend fun hold(
        record: SequencedRecord,
        reason: HoldReason,
        waitsFor: String? = null,
    ) {
        database.sync_pendingQueries.hold(
            recordKey = record.record.key,
            record = ProtoBuf.encodeToByteArray(SequencedRecord.serializer(), record),
            reason = reason.stored,
            waitsFor = waitsFor,
            receivedAt = now(),
            kind = record.record.kind,
            heldOn = appVersion.takeIf { reason.waitsForUpdate },
        )
    }

    private fun see(timestamp: HybridTimestamp) {
        if (newestSeen.let { it == null || timestamp > it }) newestSeen = timestamp
    }

    override suspend fun profileId(uuid: String): Resolved {
        database.sync_applyQueries.profileId(uuid).awaitAsOneOrNull()?.let { return Resolved.Found(it) }
        return resolve(ProfileKind.key(uuid)) { ref ->
            database.sync_applyQueries.profileId(ref.text(0)).awaitAsOneOrNull()
        }
    }

    override suspend fun entryId(entryKey: String): Resolved = resolve(entryKey) { ref ->
        ref.long(0).takeIf { database.sync_applyQueries.entryExists(it).awaitAsOne() }
    }

    override suspend fun chapterId(chapterKey: String): Resolved = resolve(chapterKey) { ref ->
        ref.long(0).takeIf { database.sync_applyQueries.chapterExists(it).awaitAsOne() }
    }

    override suspend fun categoryId(categoryKey: String, profileUuid: String): Resolved {
        // The default category is no record's row
        val defaultCategory = RecordKeys.derive(LocalReads.DEFAULT_CATEGORY, profileUuid)
        if (categoryKey == defaultCategory) return Resolved.Found(0)
        val resolved = resolve(categoryKey) { ref ->
            ref.long(0).takeIf { database.sync_applyQueries.categoryExists(it).awaitAsOne() }
        }
        return if (resolved is Resolved.Missing && resolved.key == defaultCategory) Resolved.Found(0) else resolved
    }

    override suspend fun activitySession(sessionId: String): Resolved {
        if (database.sync_applyQueries.activitySessionExists(sessionId).awaitAsOne()) return Resolved.Found(0)
        return resolve(RecordKeys.derive(ActivitySessionKind.name, sessionId)) { ref ->
            0L.takeIf { database.sync_applyQueries.activitySessionExists(ref.text(0)).awaitAsOne() }
        }
    }

    /** Registers like [tachiyomi.data.source.identity.SourceIdentityRepositoryImpl], inside this transaction. */
    override suspend fun sourceKey(source: SourceReference): Long {
        val queries = database.source_identitiesQueries
        suspend fun find() = queries.findByIdentity(source.idScheme, source.externalId).awaitAsOneOrNull()?.local_key
        find()?.let { return it }
        if (SourceIdScheme(source.idScheme).externalIdIsLocalKey) {
            queries.insertKeepingExternalId(externalId = source.externalId, idScheme = source.idScheme)
            find()?.let { return it }
        }
        queries.insertAllocatingKey(idScheme = source.idScheme, externalId = source.externalId)
        return checkNotNull(find())
    }

    override suspend fun replacementOf(entryKey: String): String? = replacement(entryKey)?.to

    override suspend fun movedTo(entryKey: String): String? =
        replacement(entryKey)?.takeIf { it.cause == Replacement.Cause.PROFILE_MOVE }?.to

    private suspend fun replacement(entryKey: String): Replacement? {
        val stored = database.sync_recordsQueries.getByKey(EntryReplacementKind.key(entryKey)).awaitAsOneOrNull()
        if (stored == null || stored.deleted) return null
        return EntryReplacementKind.decode(stored.payload).replacement.value
    }

    override suspend fun newEdit(): HybridTimestamp {
        val clock = database.sync_clockQueries.get().awaitAsOne()
        val seen = listOfNotNull(newestSeen, HybridTimestamp(clock.seen_physical, clock.seen_counter.toInt(), device))
            .max()
        val stamper = EditStamper(device, clock.issued_physical, clock.issued_counter.toInt())
        val stamp = stamper.stamp(now(), seen.physicalMillis, seen.counter)
        database.sync_clockQueries.setIssued(stamper.issuedPhysical, stamper.issuedCounter.toLong())
        return stamp
    }

    override suspend fun <P : Any> change(kind: SyncedKind<P>, key: String, payload: P) {
        val stored = database.sync_recordsQueries.getByKey(key).awaitAsOneOrNull()
        val changed = RecordState.Live(kind.encode(payload, stored?.payload))
        val merged = stored?.let { kind.merge(RecordState.of(it.deleted, it.payload), changed) } ?: changed
        if (stored != null && kind.sameRecord(merged, RecordState.of(stored.deleted, stored.payload))) return
        database.sync_recordsQueries.storeLocalChange(
            recordKey = key,
            kind = kind.name,
            localRef = stored?.local_ref,
            kindVersion = kind.version.toLong(),
            payload = merged.bytes,
            deleted = merged.deleted,
            changeId = newChangeId(),
            recordGroup = RecordGroups.of(kind.name, key, merged.deleted, merged.bytes),
        )
        holdBackIfOff(kind, key, merged)
        settle(kind, key, write(kind, key, merged, stored?.local_ref), stored?.local_ref, record = null)
    }

    /**
     * Merges [restored], the record [key] as a restore left its row at [localRef], into this device's copy, and writes
     * the result back to the row: what changed elsewhere since the backup stays, and what the backup brings that is
     * newer than this device's copy is pushed.
     */
    suspend fun <P : Any> restored(kind: SyncedKind<P>, key: String, localRef: String?, restored: RecordState) {
        val stored = database.sync_recordsQueries.getByKey(key).awaitAsOneOrNull()
        val storedState = stored?.let { RecordState.of(it.deleted, it.payload) }
        val merged = storedState?.let { kind.merge(it, restored) } ?: restored
        val changed = storedState == null || !kind.sameRecord(merged, storedState)
        if (!changed && kind.sameRecord(merged, restored)) return
        val ref = stored?.local_ref ?: localRef
        if (changed) {
            database.sync_recordsQueries.storeLocalChange(
                recordKey = key,
                kind = kind.name,
                localRef = ref,
                kindVersion = kind.version.toLong(),
                payload = merged.bytes,
                deleted = merged.deleted,
                changeId = newChangeId(),
                recordGroup = RecordGroups.of(kind.name, key, merged.deleted, merged.bytes),
            )
            holdBackIfOff(kind, key, merged)
        }
        settle(kind, key, write(kind, key, merged, ref), ref, record = null)
    }

    private suspend fun holdBackIfOff(kind: SyncedKind<*>, key: String, state: RecordState) {
        if (parts.isOff(kind, state.deleted, state.bytes)) database.sync_recordsQueries.holdBack(key)
    }

    /**
     * Brings the records here in line with what this device chose to sync: the changes of parts it stopped syncing
     * are held back from pushing, those of parts it syncs again are let go, and the records that arrived for those
     * parts while they were off are taken in, merged by edit time like any others.
     */
    suspend fun followChoices() {
        holdBackOffChanges()
        val records = database.sync_recordsQueries
        records.heldBack().awaitAsList().forEach { held ->
            val kind = SyncKinds.named(held.kind)
            if (kind == null || !parts.isOff(kind, held.deleted, held.payload)) records.letGo(held.record_key)
        }
        database.sync_pendingQueries.turnedOff().awaitAsList().forEach { held ->
            val record = ProtoBuf.decodeFromByteArray(SequencedRecord.serializer(), held.record)
            val kind = SyncKinds.named(record.record.kind)
            if (kind != null && parts.isOff(kind, record.record.deleted, record.record.payload)) return@forEach
            database.sync_pendingQueries.release(held.record_key)
            receive(record)
        }
    }

    /** Holds back the changes waiting to be pushed that belong to parts this device doesn't sync. */
    suspend fun holdBackOffChanges() {
        val records = database.sync_recordsQueries
        records.pushable().awaitAsList().forEach { change ->
            val kind = SyncKinds.named(change.kind) ?: return@forEach
            if (parts.isOff(kind, change.deleted, change.payload)) records.holdBack(change.record_key)
        }
    }

    /**
     * Removes the account profile [uuid], which this device stopped syncing, from this device as deleting it here
     * does, but with no deletion to push: the rows go inside this write, whose capture is dropped, and the profile's
     * records stay, let go of their rows, so [restoreProfile] can write them again.
     */
    suspend fun removeProfile(uuid: String) {
        val profileId = database.sync_applyQueries.profileId(uuid).awaitAsOneOrNull() ?: return
        check(profileId != activeProfileId) { "The open profile can't leave this device" }
        forEachRecordOf(uuid) { key, _ -> database.sync_recordsQueries.setLocalRef(localRef = null, recordKey = key) }
        database.sync_filesQueries.releaseProfile(fontOwnerPrefix = "[$profileId,%", profileId = profileId)
        profileDeletion.delete(profileId)
    }

    /** Takes in the records of the profile [uuid] the person said comes here ([OffParts.admit]). */
    fun admitProfile(uuid: String) = parts.admit(uuid)

    /** Writes every record of the profile [uuid] here again, once this device syncs the profile. */
    suspend fun restoreProfile(uuid: String) {
        reapply(ProfileKind.key(uuid))
        forEachRecordOf(uuid) { key, deleted -> if (!deleted) reapply(key) }
    }

    /**
     * Calls [action] with every record of the profile [uuid]: those it holds outside its titles, then each of its
     * titles followed by the records in the title's group. Titles' groups hold most of an account's records, so reading
     * only this profile's spares decoding every other profile's chapters and progress.
     */
    private suspend fun forEachRecordOf(uuid: String, action: suspend (key: String, deleted: Boolean) -> Unit) {
        val records = database.sync_recordsQueries
        suspend fun visit(key: String, kindName: String, deleted: Boolean, payload: ByteArray) {
            val kind = SyncKinds.named(kindName) ?: return
            if (parts.profileOf(kind, deleted, payload) == uuid) action(key, deleted)
        }
        var after = ""
        while (true) {
            val page = records.outsideTitlesAfter(RecordGroups.PROFILES, EntryKind.name, after, PAGE).awaitAsList()
            page.forEach { visit(it.record_key, it.kind, it.deleted, it.payload) }
            after = page.lastOrNull()?.record_key ?: break
        }
        after = ""
        while (true) {
            val page = records.kindPageAfter(EntryKind.name, after, PAGE).awaitAsList()
            page.forEach { title ->
                if (parts.profileOf(EntryKind, title.deleted, title.payload) != uuid) return@forEach
                action(title.record_key, title.deleted)
                records.inGroup(RecordGroups.entry(title.record_key)).awaitAsList()
                    .filter { it.record_key != title.record_key }
                    .forEach { visit(it.record_key, it.kind, it.deleted, it.payload) }
            }
            after = page.lastOrNull()?.record_key ?: break
        }
    }

    /**
     * Drops the changes this device made to [area]'s settings while it didn't sync them, for the account's settings,
     * which the records held for the area bring when [followChoices] takes them in.
     */
    suspend fun keepAccountSettings(area: SettingsArea) = keepAccountSettings { it.settingsArea == area }

    /** [keepAccountSettings] for the preferences [keys], in every profile. */
    suspend fun keepAccountSettings(keys: Set<String>) = keepAccountSettings { it.key in keys }

    /**
     * Drops the changes held back of the settings [matching] picks that this device syncs now. A setting the account
     * had no copy of keeps this device's value, unless one arrived meanwhile.
     */
    private suspend fun keepAccountSettings(matching: (Setting) -> Boolean) {
        val records = database.sync_recordsQueries
        records.heldBack().awaitAsList().forEach { held ->
            val kind = SyncKinds.named(held.kind) ?: return@forEach
            val setting = kind.settingOf(held.deleted, held.payload)
            if (setting == null || !matching(setting) || parts.isOff(kind, held.deleted, held.payload)) return@forEach
            when {
                held.base_payload != null -> {
                    records.takeBase(held.record_key)
                    reapply(held.record_key)
                }
                database.sync_pendingQueries.get(held.record_key).awaitAsOneOrNull() != null ->
                    records.forget(held.record_key)
            }
        }
    }

    /**
     * Whether this device changed settings it doesn't sync now that the choices [turnOn] makes would sync, such as an
     * area's or one setting's switched back on, while the account has values of its own for them, which it then asks
     * about. A setting the account has no value for keeps this device's either way ([keepAccountSettings]).
     */
    suspend fun changedWhileOff(turnOn: (ChoiceSet) -> ChoiceSet): Boolean {
        val turnedOn = OffParts(turnOn(parts.choices), database, parts.viewerSettings)
        return database.sync_recordsQueries.heldBack().awaitAsList().any { held ->
            val kind = SyncKinds.named(held.kind) ?: return@any false
            held.change_id != null &&
                kind.settingOf(held.deleted, held.payload) != null &&
                !turnedOn.isOff(kind, held.deleted, held.payload) &&
                accountHasValue(held.record_key, held.base_payload)
        }
    }

    /**
     * Whether the account has its own copy of the held record [key]: the copy this device last took in ([basePayload]),
     * or one that arrived while it held the record back.
     */
    private suspend fun accountHasValue(key: String, basePayload: ByteArray?): Boolean =
        basePayload != null || database.sync_pendingQueries.get(key).awaitAsOneOrNull() != null

    /**
     * The local ID of the record [key], or of the one it stands for ([standing]): of the row it is stored as holding,
     * or else of the row written from the stored record now. Missing when no record is stored, or when the record
     * itself waits for its own parent.
     */
    private suspend fun resolve(key: String, idOf: suspend (LocalRef) -> Long?): Resolved =
        when (val standing = database.standing(key)) {
            is Standing.Live -> resolveLive(standing.key, standing.record.local_ref, idOf)
            is Standing.Missing -> Resolved.Missing(standing.key)
            Standing.Gone -> Resolved.Gone
        }

    private suspend fun resolveLive(key: String, localRef: String?, idOf: suspend (LocalRef) -> Long?): Resolved {
        localRef?.let { idOf(LocalRef(it)) }?.let { return Resolved.Found(it) }
        if (!resolving.add(key)) return Resolved.Missing(key)
        try {
            return when (val outcome = reapply(key, database.sync_pendingQueries.get(key).awaitAsOneOrNull())) {
                is ApplyOutcome.Applied -> outcome.ref?.let { idOf(it) }?.let(Resolved::Found) ?: Resolved.Gone
                is ApplyOutcome.WaitsFor -> Resolved.Missing(outcome.key)
                is ApplyOutcome.Held, null -> Resolved.Missing(key)
                is ApplyOutcome.Superseded -> Resolved.Gone
            }
        } finally {
            resolving -= key
        }
    }

    private companion object {
        const val PAGE = 500L
    }
}

private fun <P : Any> SyncedKind<P>.newestEdit(state: RecordState): HybridTimestamp? = when (state) {
    is RecordState.Live -> newestEdit(serializer, decode(state.bytes))
    is RecordState.Deleted -> state.tombstone.deletedAt
}

/** Which setting a record is: its Settings area, and the preference key for an app preference. */
private class Setting(val settingsArea: SettingsArea, val key: String?)

private fun <P : Any> SyncedKind<P>.settingOf(deleted: Boolean, bytes: ByteArray): Setting? {
    val live = RecordState.of(deleted, bytes) as? RecordState.Live ?: return null
    val payload = decode(live.bytes)
    return settingsArea(payload)?.let { Setting(it, settingKey(payload)) }
}

private fun <P : Any> SyncedKind<P>.sameRecord(a: RecordState, b: RecordState): Boolean = when {
    a is RecordState.Live && b is RecordState.Live -> decode(a.bytes) == decode(b.bytes)
    a is RecordState.Deleted && b is RecordState.Deleted -> a.tombstone == b.tombstone
    else -> false
}
