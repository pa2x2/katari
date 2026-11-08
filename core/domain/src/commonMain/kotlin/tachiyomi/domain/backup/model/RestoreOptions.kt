package tachiyomi.domain.backup.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*

/** What to restore. [profileUuids] picks which of a backup's profiles to restore; null restores all of them. */
data class RestoreOptions(
    val libraryEntries: Boolean = true,
    val categories: Boolean = true,
    val appSettings: Boolean = true,
    val extensionStores: Boolean = true,
    val sourceSettings: Boolean = true,
    val profileUuids: Set<String>? = null,
    /**
     * Whether every device of the sync account returns to the backup (D046a), rather than this one filling in what
     * is older or missing; only offered while the device syncs, with every part and profile restored.
     */
    val rollBackAccount: Boolean = false,
) {

    fun asBooleanArray() = booleanArrayOf(
        libraryEntries,
        categories,
        appSettings,
        extensionStores,
        sourceSettings,
    )

    fun canRestore() = libraryEntries || categories || appSettings || extensionStores || sourceSettings

    companion object {
        val options = listOf(
            Entry(
                label = MR.strings.label_library,
                getter = RestoreOptions::libraryEntries,
                setter = { options, enabled -> options.copy(libraryEntries = enabled) },
            ),
            Entry(
                label = MR.strings.categories,
                getter = RestoreOptions::categories,
                setter = { options, enabled -> options.copy(categories = enabled) },
            ),
            Entry(
                label = MR.strings.app_settings,
                getter = RestoreOptions::appSettings,
                setter = { options, enabled -> options.copy(appSettings = enabled) },
            ),
            Entry(
                label = MR.strings.extensionStores,
                getter = RestoreOptions::extensionStores,
                setter = { options, enabled -> options.copy(extensionStores = enabled) },
            ),
            Entry(
                label = MR.strings.source_settings,
                getter = RestoreOptions::sourceSettings,
                setter = { options, enabled -> options.copy(sourceSettings = enabled) },
            ),
        )

        fun fromBooleanArray(array: BooleanArray, profileUuids: Set<String>?, rollBackAccount: Boolean) =
            RestoreOptions(
                libraryEntries = array[0],
                categories = array[1],
                appSettings = array[2],
                extensionStores = array[3],
                sourceSettings = array[4],
                profileUuids = profileUuids,
                rollBackAccount = rollBackAccount,
            )
    }

    data class Entry(
        val label: StringResource,
        val getter: (RestoreOptions) -> Boolean,
        val setter: (RestoreOptions, Boolean) -> RestoreOptions,
    )
}
