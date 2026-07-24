package mihon.feature.migration.options

import dev.icerock.moko.resources.StringResource
import mihon.domain.migration.models.MigrationFlag
import tachiyomi.i18n.MR

internal fun MigrationFlag.getLabel(): StringResource {
    return when (this) {
        MigrationFlag.CHAPTER -> MR.strings.items
        MigrationFlag.CATEGORY -> MR.strings.categories
        MigrationFlag.CUSTOM_COVER -> MR.strings.custom_cover
        MigrationFlag.NOTES -> MR.strings.action_notes
        MigrationFlag.REMOVE_DOWNLOAD -> MR.strings.delete_downloaded
    }
}
