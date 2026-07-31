package mihon.entry.interactions.download.configuration.backup

import tachiyomi.domain.entry.model.Entry

interface EntryDownloadConfigurationBackupFeature {
    suspend fun snapshot(entry: Entry): EntryDownloadConfigurationBackupState?

    suspend fun restore(entry: Entry, state: EntryDownloadConfigurationBackupState)
}
