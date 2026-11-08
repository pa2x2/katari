package app.katari.sync.client.apply

import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.kind.SyncKinds
import app.katari.sync.client.record.RecordGroups
import app.katari.sync.protocol.pull.PullPage
import app.katari.sync.protocol.record.SequencedRecord
import dev.zacsweers.metro.Inject

/**
 * Where records pulled from the server enter the device. Transport calls [apply] with each page in the order the
 * server hands them out, and [retryHeld] at the start of each sync.
 *
 * Contract:
 * - A page is applied in one transaction together with the device's new position, so a sync interrupted anywhere
 *   resumes from the last page applied and never applies a page twice or half of one.
 * - Applying is idempotent: a page applied again, or records arriving in another order or batching, leave the same
 *   data, since merging is.
 * - Records within a page are applied parents first (profiles, entries, chapters, …, in [SyncKinds.all] order); a
 *   record whose parent comes in a later page waits for it.
 * - Edits on this device not drained into records yet are merged, not overwritten, and nothing applied is captured
 *   as a local edit ([SyncWriting]).
 * - A record that fails to apply is held aside and the rest apply, page and position with them
 *   ([SyncWriting.writeHoldingFailures]); it is tried again once the app is updated.
 * - What is left to push afterwards is in [app.katari.sync.client.record.PendingChanges]: records this device has
 *   edits of that the server's copy lacks, never records only applied here.
 */
@Inject
class IncomingRecords(private val writing: SyncWriting) {
    suspend fun apply(page: PullPage) = writing.writeHoldingFailures { session ->
        page.records
            .sortedBy { kindOrder[it.record.kind] ?: Int.MAX_VALUE }
            .forEach { session.receive(it) }
        sync_stateQueries.setPosition(
            serverInstanceId = page.server.instanceId,
            cursor = page.nextCursor,
            chain = page.nextChain,
            downloadingStages = DOWNLOADING_STAGES,
            serverSequence = page.server.sequence,
        )
    }

    /**
     * Applies [records] fetched ahead of the device's position, a group of them ([RecordGroups]), leaving the position
     * where it is. Merging is idempotent and order-free, and the server hands out only a record's newest copy, so
     * the pull that reaches them later applies the same copies again, or newer ones, and changes nothing else.
     */
    suspend fun applyAhead(records: List<SequencedRecord>) = writing.writeHoldingFailures { session ->
        records
            .sortedBy { kindOrder[it.record.kind] ?: Int.MAX_VALUE }
            .forEach { session.receive(it) }
    }

    /** Applies what was held for a reason that may have passed: an app update, the clock, a reference, a profile. */
    suspend fun retryHeld() = writing.writeHoldingFailures { session -> session.retryHeld() }

    private companion object {
        val kindOrder = SyncKinds.all.withIndex().associate { (index, kind) -> kind.name to index }
        val DOWNLOADING_STAGES = SyncStage.entries.filter { it.isDownloading }.map { it.stored }
    }
}
