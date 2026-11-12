package app.katari.sync.client.resync

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.apply.SyncWriting
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.kind.SyncKinds
import app.katari.sync.client.merge.newestEdit
import app.katari.sync.client.payload.Tombstone
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.client.record.newChangeId
import app.katari.sync.protocol.clock.HybridTimestamp
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.record.RecordEnvelope
import app.katari.sync.protocol.record.SequencedRecord
import app.katari.sync.protocol.server.ServerIdentity
import dev.zacsweers.metro.Inject
import kotlinx.serialization.protobuf.ProtoBuf
import tachiyomi.data.DatabaseHandler

/**
 * A full resync (D011, D048), for when the device's position means nothing on the server: the server was replaced by
 * an empty one or restored from an old copy ([SyncStage.RESYNC]), or no longer keeps the deletions made since the
 * device's position ([SyncStage.RESYNC_WITHOUT_DELETIONS]).
 *
 * [start] forgets which server copy every record was based on and moves the position back to 0, in one transaction.
 * The pull from 0 then merges every record the server has, which bases each one it brings on the server's copy again
 * and pushes whatever this device holds that the server's copy lacks. [finish] deals with the records the pull didn't
 * bring, which are still based on nothing:
 * - From a lost or restored server, every one is pushed: the server lost it, and the device's copy, merged from every
 *   device before the loss, is the newest there is. So the first device of a replaced server fills it, and the next
 *   ones merge into what it holds; a record two devices push is merged by the server's stale check like any edit.
 * - From a server that dropped deletions, a record the device had synced and hasn't changed since was deleted
 *   elsewhere, and is deleted here without pushing; one it changed since is pushed, the change outliving a deletion it
 *   can't be compared with; one never synced is pushed as it would have been.
 *
 * A first download that starts over ([restartDownload]) settles the records it took before and doesn't get back the
 * same way, as the resync it starts over for would ([finishDownload]). Neither touches the rows only this device held,
 * which were never synced and wait to be pushed already.
 *
 * Records of parts of the account this device doesn't sync wait to be pushed until it syncs them again.
 *
 * Pushes are of merged copies and carry change ids, so pushing a record again changes nothing on any device. Every
 * custom cover and font in place here is offered to the server again, which uploads those a lost server lacks, and
 * files the server reported missing are asked for again, as other devices may have uploaded them since.
 */
@Inject
class FullResync(
    private val handler: DatabaseHandler,
    private val writing: SyncWriting,
) {
    /** Starts a resync at [stage]. */
    suspend fun start(stage: SyncStage, server: ServerIdentity) {
        require(stage == SyncStage.RESYNC || stage == SyncStage.RESYNC_WITHOUT_DELETIONS) { "$stage is no resync" }
        handler.await(inTransaction = true) {
            sync_recordsQueries.forgetBases()
            sync_stateQueries.restartPosition(stage.stored, server.sequence, server.instanceId)
        }
    }

    /**
     * Starts a first download over, for what a resync at [reason] starts for. A lost server outweighs an expired
     * position when the download starts over again: the copies it lost may be among the records taken before, and
     * can't be told apart from the deletions the expired position missed, so they are pushed, as a resync does.
     */
    suspend fun restartDownload(reason: SyncStage, server: ServerIdentity) {
        handler.await(inTransaction = true) {
            val before = sync_stateQueries.get().awaitAsOne().download_restarted_as?.let(SyncStage::of)
            val restartedAs = if (before == SyncStage.RESYNC) before else reason
            sync_recordsQueries.forgetBases()
            sync_stateQueries.restartPosition(SyncStage.DOWNLOADING.stored, server.sequence, server.instanceId)
            sync_stateQueries.setDownloadRestartedAs(restartedAs.stored)
        }
    }

    /** Ends a resync at [stage]. */
    suspend fun finish(stage: SyncStage) = settle(stage, next = SyncStage.READY)

    /**
     * Ends a first download, which goes on to the device's settings. One that started over settles the records it
     * took before as a resync at [restartedAs] does.
     */
    suspend fun finishDownload(restartedAs: SyncStage?) {
        if (restartedAs == null) {
            handler.await { sync_stateQueries.setStage(SyncStage.SETTINGS.stored, 0) }
        } else {
            settle(restartedAs, next = SyncStage.SETTINGS)
        }
    }

    private suspend fun settle(stage: SyncStage, next: SyncStage) {
        if (stage == SyncStage.RESYNC_WITHOUT_DELETIONS) deleteWhatWasDeletedElsewhere()
        writing.write { session ->
            sync_recordsQueries.pushUnseen()
            session.holdBackOffChanges()
            sync_filesQueries.queuePlaced()
            sync_filesQueries.forgetUnavailable()
            sync_stateQueries.setStage(next.stored, 0)
        }
    }

    private suspend fun deleteWhatWasDeletedElsewhere() {
        val missing = handler.await { sync_recordsQueries.deletedElsewhere().awaitAsList() }
        val deletions = missing.mapNotNull { record ->
            val kind = SyncKinds.named(record.kind)?.takeIf { it.absence == SyncedKind.Absence.TOMBSTONE }
                ?: return@mapNotNull null
            deletion(kind, record.record_key, record.payload)
        }
        if (deletions.isEmpty()) return
        writing.write { session -> deletions.forEach { session.receive(it) } }
    }

    // A deletion after every edit the device's copy holds, so merging it deletes the record, and the merged copy is
    // the deletion itself, which leaves nothing to push.
    private fun <P : Any> deletion(kind: SyncedKind<P>, key: String, payload: ByteArray): SequencedRecord {
        val newest = newestEdit(kind.serializer, kind.decode(payload))
        val deletedAt = HybridTimestamp(
            physicalMillis = newest?.physicalMillis ?: 0,
            counter = (newest?.counter ?: 0) + 1,
            device = DeviceId(0),
        )
        val tombstone = Tombstone(deletedAt = deletedAt, last = payload)
        return SequencedRecord(
            sequence = 0,
            record = RecordEnvelope(
                key = key,
                kind = kind.name,
                kindVersion = kind.version,
                changeId = newChangeId(),
                device = DeviceId(0),
                deleted = true,
                payload = ProtoBuf.encodeToByteArray(Tombstone.serializer(), tombstone),
            ),
        )
    }
}
