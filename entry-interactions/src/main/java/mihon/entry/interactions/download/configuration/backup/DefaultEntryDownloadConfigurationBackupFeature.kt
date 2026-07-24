package mihon.entry.interactions

import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.VideoDownloadQualityMode
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository

internal class DefaultEntryDownloadConfigurationBackupFeature(
    evaluation: FeatureGraphEvaluation,
    private val repository: DownloadPreferencesRepository,
) : EntryDownloadConfigurationBackupFeature {
    private val applicableTypes = evaluation.applicableProviderTypes<EntryDownloadOptionsProcessor>(
        feature = ENTRY_DOWNLOAD_CONFIGURATION_FEATURE_ID,
        integration = ENTRY_DOWNLOAD_OPTIONS_INTEGRATION_ID,
        behaviorProjection = ENTRY_DOWNLOAD_OPTIONS_BEHAVIOR_ID,
    )

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
