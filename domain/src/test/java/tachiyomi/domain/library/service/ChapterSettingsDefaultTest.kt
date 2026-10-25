package tachiyomi.domain.library.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.entry.model.Entry

class ChapterSettingsDefaultTest {

    @Test
    fun `defaults applied to an entry keep every chapter setting that was saved as default`() {
        val preferences = LibraryPreferences(InMemoryPreferenceStore())
        val configured = Entry.create().copy(
            chapterFlags = Entry.CHAPTER_SHOW_UNREAD or
                Entry.CHAPTER_SHOW_NOT_DOWNLOADED or
                Entry.CHAPTER_SHOW_BOOKMARKED or
                Entry.CHAPTER_SHOW_NOT_FILLER or
                Entry.CHAPTER_HIDE_PREVIEWS or
                Entry.CHAPTER_SORTING_ALPHABET or
                Entry.CHAPTER_SORT_ASC or
                Entry.CHAPTER_DISPLAY_NUMBER,
        )

        preferences.setChapterSettingsDefault(configured)

        preferences.defaultChapterFlags() shouldBe configured.chapterFlags
    }
}
