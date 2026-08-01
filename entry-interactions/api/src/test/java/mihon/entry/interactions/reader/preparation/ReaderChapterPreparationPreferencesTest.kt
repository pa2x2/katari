package mihon.entry.interactions.reader.preparation

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class ReaderChapterPreparationPreferencesTest {
    @Test
    fun `preparation is opt in`() {
        ReaderChapterPreparationPreferences(InMemoryPreferenceStore())
            .prepareNextChapter("builtin.book.document")
            .get() shouldBe false
    }

    @Test
    fun `legacy enabled value seeds independent reader surfaces`() {
        val preferences = ReaderChapterPreparationPreferences(
            InMemoryPreferenceStore(
                sequenceOf(
                    InMemoryPreferenceStore.InMemoryPreference(
                        ReaderChapterPreparationPreferences.LEGACY_PREPARE_NEXT_CHAPTER_KEY,
                        true,
                        false,
                    ),
                ),
            ),
        )
        val book = preferences.prepareNextChapter("builtin.book.document")
        val manga = preferences.prepareNextChapter("builtin.manga.reader")

        preferences.completeLegacyMigration(
            setOf("builtin.book.document", "builtin.manga.reader"),
        )

        book.get() shouldBe true
        manga.get() shouldBe true
        book.set(false)
        book.get() shouldBe false
        manga.get() shouldBe true
    }

    @Test
    fun `legacy migration does not overwrite an existing surface value`() {
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

        preferences.prepareNextChapter("builtin.book.document").get() shouldBe false
    }
}
