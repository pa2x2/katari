package app.katari.sourcemigration.options

import app.katari.entry.interactions.migration.EntryMigrationOption
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*

internal fun EntryMigrationOption.getLabel(): StringResource {
    return when (this) {
        EntryMigrationOption.CHILD_STATE -> MR.strings.items
        EntryMigrationOption.CATEGORIES -> MR.strings.categories
        EntryMigrationOption.CUSTOM_COVER -> MR.strings.custom_cover
        EntryMigrationOption.NOTES -> MR.strings.action_notes
        EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS -> MR.strings.delete_downloaded
    }
}
