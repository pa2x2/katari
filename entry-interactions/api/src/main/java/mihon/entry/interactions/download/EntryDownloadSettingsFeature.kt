package mihon.entry.interactions

/** Feature-owned boundary for specialized downloader settings exposed by contributed providers. */
interface EntryDownloadSettingsFeature {
    val availableSettings: Set<EntryDownloadSetting>

    fun isAvailable(setting: EntryDownloadSetting): Boolean = setting in availableSettings
}

enum class EntryDownloadSetting {
    ARCHIVE_PACKAGING,
    TALL_IMAGE_SPLITTING,
    PARALLEL_SOURCE_TRANSFERS,
    PARALLEL_ITEM_TRANSFERS,
}
