package mihon.entry.interactions.reader.preparation

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class ReaderChapterPreparationPreferences(
    preferenceStore: PreferenceStore,
) {
    val prepareNextChapter: Preference<Boolean> = preferenceStore.getBoolean(PREPARE_NEXT_CHAPTER_KEY, false)

    companion object {
        const val PREPARE_NEXT_CHAPTER_KEY = "reader_prepare_next_chapter"
    }
}

object ReaderChapterPreparationPolicy {
    const val THRESHOLD = 0.75

    fun shouldPrepare(
        enabled: Boolean,
        progression: Double,
    ): Boolean = enabled && progression >= THRESHOLD
}
