package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryDownloadConfigurationBackupFeature {
    suspend fun snapshot(entry: Entry): EntryDownloadConfigurationBackupState?

    suspend fun restore(entry: Entry, state: EntryDownloadConfigurationBackupState)
}
