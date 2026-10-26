package eu.kanade.tachiyomi.source.lnreader.entry

import eu.kanade.tachiyomi.source.entry.SEntry

/**
 * A novel's status: LNReader's own names, where `STUB` marks a novel taken down for its license and `Inactive` one that
 * stopped, plus what plugins of other languages send for an ongoing novel. Anything else is unknown, as in LNReader.
 */
internal fun lnReaderStatus(status: String?): Int = when (status?.trim()?.lowercase()) {
    "ongoing", "active", "ativo", "devam ediyor", "مستمرة" -> SEntry.ONGOING
    "completed" -> SEntry.COMPLETED
    "licensed", "stub" -> SEntry.LICENSED
    "publishing finished" -> SEntry.PUBLISHING_FINISHED
    "cancelled" -> SEntry.CANCELLED
    "on hiatus", "inactive" -> SEntry.ON_HIATUS
    else -> SEntry.UNKNOWN
}
