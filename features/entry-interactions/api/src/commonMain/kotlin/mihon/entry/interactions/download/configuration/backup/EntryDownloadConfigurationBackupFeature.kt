package mihon.entry.interactions.download.configuration.backup

import tachiyomi.domain.entry.model.Entry

interface EntryDownloadConfigurationBackupFeature {
    /** The download configuration of each of [entries] that has one, by entry id. */
    suspend fun snapshot(entries: List<Entry>): Map<Long, EntryDownloadConfigurationBackupState>

    suspend fun restore(entry: Entry, state: EntryDownloadConfigurationBackupState)
}
