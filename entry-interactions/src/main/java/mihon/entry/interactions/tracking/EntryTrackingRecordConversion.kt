package mihon.entry.interactions

import tachiyomi.domain.track.model.EntryTrack

internal fun EntryTrack.toTrackingRecord() = EntryTrackingRecord(
    id = id,
    entryId = entryId,
    serviceId = EntryTrackingServiceId(trackerId),
    remoteId = remoteId,
    libraryId = libraryId,
    title = title,
    progress = progress,
    total = total,
    status = status,
    score = score,
    remoteUrl = remoteUrl,
    startDate = startDate,
    finishDate = finishDate,
    private = private,
)

internal fun EntryTrackingRecord.toDomainTrack() = EntryTrack(
    id = id,
    entryId = entryId,
    trackerId = serviceId.value,
    remoteId = remoteId,
    libraryId = libraryId,
    title = title,
    progress = progress,
    total = total,
    status = status,
    score = score,
    remoteUrl = remoteUrl,
    startDate = startDate,
    finishDate = finishDate,
    private = private,
)
