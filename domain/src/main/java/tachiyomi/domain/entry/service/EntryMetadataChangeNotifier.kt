package tachiyomi.domain.entry.service

import tachiyomi.domain.entry.model.Entry

/** Host-neutral port for publishing a successfully persisted Entry metadata transition. */
fun interface EntryMetadataChangeNotifier {
    suspend fun changed(previous: Entry, current: Entry)
}
