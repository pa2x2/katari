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
}
