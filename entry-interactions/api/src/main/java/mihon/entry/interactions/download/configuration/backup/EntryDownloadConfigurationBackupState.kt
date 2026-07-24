package mihon.entry.interactions

import kotlinx.serialization.Serializable

const val ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_STATE_ID = "entry.download.configuration.backup"
const val ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_SCHEMA_VERSION = 1

@Serializable
data class EntryDownloadConfigurationBackupState(
    val dubKey: String? = null,
    val streamKey: String? = null,
    val subtitleKey: String? = null,
    val qualityMode: EntryDownloadConfigurationQualityMode = EntryDownloadConfigurationQualityMode.BALANCED,
    val updatedAt: Long = 0,
)

@Serializable
enum class EntryDownloadConfigurationQualityMode {
    BEST,
    BALANCED,
    DATA_SAVING,
}
