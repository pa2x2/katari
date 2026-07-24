package mihon.feature.migration.options

import mihon.domain.migration.models.MigrationFlag
import mihon.entry.interactions.EntryMigrationOption

internal fun MigrationFlag.toEntryMigrationOption(): EntryMigrationOption {
    return when (this) {
        MigrationFlag.CHAPTER -> EntryMigrationOption.CHILD_STATE
        MigrationFlag.CATEGORY -> EntryMigrationOption.CATEGORIES
        MigrationFlag.CUSTOM_COVER -> EntryMigrationOption.CUSTOM_COVER
        MigrationFlag.NOTES -> EntryMigrationOption.NOTES
        MigrationFlag.REMOVE_DOWNLOAD -> EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS
    }
}

internal fun EntryMigrationOption.toMigrationFlag(): MigrationFlag {
    return when (this) {
        EntryMigrationOption.CHILD_STATE -> MigrationFlag.CHAPTER
        EntryMigrationOption.CATEGORIES -> MigrationFlag.CATEGORY
        EntryMigrationOption.NOTES -> MigrationFlag.NOTES
        EntryMigrationOption.CUSTOM_COVER -> MigrationFlag.CUSTOM_COVER
        EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS -> MigrationFlag.REMOVE_DOWNLOAD
    }
}

internal fun Set<MigrationFlag>.toEntryMigrationOptions(): Set<EntryMigrationOption> =
    mapTo(mutableSetOf(), MigrationFlag::toEntryMigrationOption)

internal fun Set<EntryMigrationOption>.toMigrationFlags(): Set<MigrationFlag> =
    mapTo(mutableSetOf(), EntryMigrationOption::toMigrationFlag)
