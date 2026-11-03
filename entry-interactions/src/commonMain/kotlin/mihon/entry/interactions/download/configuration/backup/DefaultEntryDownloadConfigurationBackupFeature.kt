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

    override suspend fun snapshot(entries: List<Entry>): Map<Long, EntryDownloadConfigurationBackupState> {
        val applicable = entries.filter { it.type in applicableTypes }
        if (applicable.isEmpty()) return emptyMap()
        return repository.getByEntryIds(applicable.map(Entry::id)).mapValues { (_, preferences) ->
            preferences.toBackupState()
        }
    }

    override suspend fun restore(entry: Entry, state: EntryDownloadConfigurationBackupState) {
        if (entry.type !in applicableTypes) return
        repository.upsert(
            DownloadPreferences(
                entryId = entry.id,
                dubKey = state.dubKey,
                serverKey = state.serverKey,
                streamKey = state.streamKey,
                subtitleKey = state.subtitleKey,
                audioKey = state.audioKey,
                qualityMode = state.qualityMode.toDomain(),
                updatedAt = state.updatedAt,
            ),
        )
    }
}

private fun DownloadPreferences.toBackupState() = EntryDownloadConfigurationBackupState(
    dubKey = dubKey,
    serverKey = serverKey,
    streamKey = streamKey,
    subtitleKey = subtitleKey,
    audioKey = audioKey,
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
