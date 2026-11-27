package tachiyomi.domain.source.lnreader

import tachiyomi.domain.entry.model.EntryStatus

/**
 * A novel's status: LNReader's own names, where `STUB` marks a novel taken down for its license and `Inactive` one that
 * stopped, plus what plugins of other languages send for an ongoing novel. Anything else is unknown, as in LNReader.
 */
fun lnReaderStatus(status: String?): EntryStatus = when (status?.trim()?.lowercase()) {
    "ongoing", "active", "ativo", "devam ediyor", "مستمرة" -> EntryStatus.ONGOING
    "completed" -> EntryStatus.COMPLETED
    "licensed", "stub" -> EntryStatus.LICENSED
    "publishing finished" -> EntryStatus.PUBLISHING_FINISHED
    "cancelled" -> EntryStatus.CANCELLED
    "on hiatus", "inactive" -> EntryStatus.ON_HIATUS
    else -> EntryStatus.UNKNOWN
}
