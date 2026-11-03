package tachiyomi.domain.library.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class GlobalLibraryPreferences(
    preferenceStore: PreferenceStore,
) {
    val markDuplicateReadChapterAsRead: Preference<Set<String>> = preferenceStore.getStringSet(
        "mark_duplicate_read_chapter_read",
        emptySet(),
    )

    val disallowNonAsciiFilenames: Preference<Boolean> = preferenceStore.getBoolean(
        "disallow_non_ascii_filenames",
        false,
    )

    // Off lets downloads keep the plain chapter or episode name, but then one named like an existing download is
    // taken for it
    val enableChapterNameHash: Preference<Boolean> = preferenceStore.getBoolean(
        "pref_enable_chapter_name_hash",
        true,
    )
}
