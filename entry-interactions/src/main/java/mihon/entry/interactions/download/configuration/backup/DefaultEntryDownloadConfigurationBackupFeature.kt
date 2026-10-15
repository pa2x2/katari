package mihon.entry.interactions.download.configuration.backup

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.VideoDownloadQualityMode
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository

internal class DefaultEntryDownloadConfigurationBackupFeature(
    private val applicableTypes: Set<EntryType>,
    private val repository: DownloadPreferencesRepository,
) : EntryDownloadConfigurationBackupFeature {

    override suspend fun snapshot(entry: Entry): EntryDownloadConfigurationBackupState? {
        if (entry.type !in applicableTypes) return null
        return repository.getByEntryId(entry.id)?.toBackupState()
    }

    override suspend fun restore(entry: Entry, state: EntryDownloadConfigurationBackupState) {
        if (entry.type !in applicableTypes) return
        repository.upsert(
            DownloadPreferences(
                entryId = entry.id,
                dubKey = state.dubKey,
                streamKey = state.streamKey,
                subtitleKey = state.subtitleKey,
                qualityMode = state.qualityMode.toDomain(),
                updatedAt = state.updatedAt,
            ),
        )
    }
}

private fun DownloadPreferences.toBackupState() = EntryDownloadConfigurationBackupState(
    dubKey = dubKey,
    streamKey = streamKey,
    subtitleKey = subtitleKey,
    qualityMode = when (qualityMode) {
        VideoDownloadQualityMode.BEST -> EntryDownloadConfigurationQualityMode.BEST
        VideoDownloadQualityMode.BALANCED -> EntryDownloadConfigurationQualityMode.BALANCED
        VideoDownloadQualityMode.DATA_SAVING -> EntryDownloadConfigurationQualityMode.DATA_SAVING
    },
    updatedAt = updatedAt,
)

private fun EntryDownloadConfigurationQualityMode.toDomain(): VideoDownloadQualityMode {
    return when (this) {
        EntryDownloadConfigurationQualityMode.BEST -> VideoDownloadQualityMode.BEST
        EntryDownloadConfigurationQualityMode.BALANCED -> VideoDownloadQualityMode.BALANCED
        EntryDownloadConfigurationQualityMode.DATA_SAVING -> VideoDownloadQualityMode.DATA_SAVING
    }
}
