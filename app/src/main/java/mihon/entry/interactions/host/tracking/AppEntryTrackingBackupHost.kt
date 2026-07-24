package mihon.entry.interactions.host.tracking

import mihon.entry.interactions.EntryTrackingBackupRecord
import tachiyomi.data.DatabaseHandler
import kotlin.math.max

internal class AppEntryTrackingBackupHost(
    private val handler: DatabaseHandler,
) : EntryTrackingBackupHost {
    override suspend fun snapshot(profileId: Long, entryId: Long): List<EntryTrackingBackupRecord> {
        return handler.awaitList {
            entry_syncQueries.getTracksByEntryId(profileId, entryId) {
                    _,
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
                EntryTrackingBackupRecord(
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
            }.executeAsList().associateBy { it.value.serviceId }

            records.forEach { incoming ->
                val existing = existingByService[incoming.serviceId]
                if (existing == null) {
                    entry_syncQueries.insert(
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
                } else if (incoming != existing.value) {
                    val merged = incoming.mergeForRestore(existing.value)
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

internal fun EntryTrackingBackupRecord.mergeForRestore(
    existing: EntryTrackingBackupRecord,
): EntryTrackingBackupRecord {
    return existing.copy(
        remoteId = remoteId,
        libraryId = libraryId,
        progress = max(existing.progress, progress),
    )
}

private data class ExistingRecord(
    val id: Long,
    val value: EntryTrackingBackupRecord,
)
