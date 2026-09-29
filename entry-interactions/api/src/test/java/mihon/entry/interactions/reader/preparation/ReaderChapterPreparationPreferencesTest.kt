package mihon.entry.interactions.reader.preparation

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class ReaderChapterPreparationPreferencesTest {
    @Test
    fun `legacy enabled value seeds reader surfaces without overwriting an existing surface value`() {
        val preferences = ReaderChapterPreparationPreferences(
            InMemoryPreferenceStore(
                sequenceOf(
                    InMemoryPreferenceStore.InMemoryPreference(
                        ReaderChapterPreparationPreferences.LEGACY_PREPARE_NEXT_CHAPTER_KEY,
                        true,
                        false,
                    ),
                    InMemoryPreferenceStore.InMemoryPreference(
                        ReaderChapterPreparationPreferences.SURFACE_KEY_PREFIX + "builtin.book.document",
                        false,
                        false,
                    ),
                ),
            ),
        )

        preferences.completeLegacyMigration(
            setOf("builtin.book.document", "builtin.manga.reader"),
        )

        preferences.prepareNextChapter("builtin.book.document").get() shouldBe false
        preferences.prepareNextChapter("builtin.manga.reader").get() shouldBe true
    }
}
