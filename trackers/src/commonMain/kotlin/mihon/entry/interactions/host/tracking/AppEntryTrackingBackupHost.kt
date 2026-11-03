package mihon.entry.interactions.host.tracking

import app.cash.sqldelight.async.coroutines.awaitAsList
import mihon.entry.interactions.tracking.backup.EntryTrackingBackupRecord
import mihon.entry.interactions.tracking.host.EntryTrackingBackupHost
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.query.chunkedForSqlQuery

internal class AppEntryTrackingBackupHost(
    private val handler: DatabaseHandler,
) : EntryTrackingBackupHost {
    override suspend fun snapshot(
        profileId: Long,
        entryIds: Collection<Long>,
    ): Map<Long, List<EntryTrackingBackupRecord>> {
        return entryIds.distinct().chunkedForSqlQuery().flatMap { chunk ->
            handler.awaitList {
                entry_syncQueries.getTracksByEntryIds(profileId, chunk) {
                        _,
                        _,
                        entryId,
                        syncId,
                        remoteId,
                        libraryId,
                        title,
                        progress,
                        total,
                        status,
                        score,
                        remoteUrl,
                        startDate,
                        finishDate,
                        private,
                    ->
                    entryId to EntryTrackingBackupRecord(
                        serviceId = syncId,
                        remoteId = remoteId,
                        libraryId = libraryId,
                        title = title,
                        progress = progress,
                        total = total,
                        score = score,
                        status = status,
                        startDate = startDate,
                        finishDate = finishDate,
                        remoteUrl = remoteUrl,
                        private = private,
                    )
                }
            }
        }
            .groupBy({ it.first }, { it.second })
    }

    override suspend fun restore(profileId: Long, entryId: Long, records: List<EntryTrackingBackupRecord>) {
        if (records.isEmpty()) return
        handler.await(inTransaction = true) {
            val existingByService = entry_syncQueries.getTracksByEntryId(profileId, entryId) {
                    id,
                    _,
                    _,
                    syncId,
                    remoteId,
                    libraryId,
                    title,
                    progress,
                    total,
                    status,
                    score,
                    remoteUrl,
                    startDate,
                    finishDate,
                    private,
                ->
                ExistingRecord(
                    id = id,
                    value = EntryTrackingBackupRecord(
                        serviceId = syncId,
                        remoteId = remoteId,
                        libraryId = libraryId,
                        title = title,
                        progress = progress,
                        total = total,
                        score = score,
                        status = status,
                        startDate = startDate,
                        finishDate = finishDate,
                        remoteUrl = remoteUrl,
                        private = private,
                    ),
                )
            }.awaitAsList().associateBy { it.value.serviceId }

            records.forEach { incoming ->
                val existing = existingByService[incoming.serviceId]
                if (existing == null) {
                    entry_syncQueries.upsert(
                        profileId = profileId,
                        entryId = entryId,
                        syncId = incoming.serviceId,
                        remoteId = incoming.remoteId,
                        libraryId = incoming.libraryId,
                        title = incoming.title,
                        lastChapterRead = incoming.progress,
                        totalChapters = incoming.total,
                        status = incoming.status,
                        score = incoming.score,
                        remoteUrl = incoming.remoteUrl,
                        startDate = incoming.startDate,
                        finishDate = incoming.finishDate,
                        private = incoming.private,
                    )
                } else {
                    val merged = incoming.mergeForRestore(existing.value) ?: return@forEach
                    entry_syncQueries.update(
                        entryId = entryId,
                        syncId = merged.serviceId,
                        mediaId = merged.remoteId,
                        libraryId = merged.libraryId,
                        title = merged.title,
                        lastChapterRead = merged.progress,
                        totalChapter = merged.total,
                        status = merged.status,
                        score = merged.score,
                        trackingUrl = merged.remoteUrl,
                        startDate = merged.startDate,
                        finishDate = merged.finishDate,
                        private = merged.private,
                        id = existing.id,
                        profileId = profileId,
                    )
                }
            }
        }
    }
}

/**
 * The stored track wins like the rest of an entry's stored data; taking only the backup's remote ids would pair one
 * remote entry's ids with another's url and title. The backup can only move progress forward; null means no change.
 */
internal fun EntryTrackingBackupRecord.mergeForRestore(
    existing: EntryTrackingBackupRecord,
): EntryTrackingBackupRecord? {
    if (progress <= existing.progress) return null
    return existing.copy(progress = progress)
}

private data class ExistingRecord(
    val id: Long,
    val value: EntryTrackingBackupRecord,
)
