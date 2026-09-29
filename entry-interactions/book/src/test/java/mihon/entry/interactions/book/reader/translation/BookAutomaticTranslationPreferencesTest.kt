package mihon.entry.interactions.book.reader.translation

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class BookAutomaticTranslationPreferencesTest {

    @Test
    fun `legacy global value seeds each reader once and readers then remain independent`() {
        val store = InMemoryPreferenceStore(
            sequenceOf(
                InMemoryPreferenceStore.InMemoryPreference(
                    BookAutomaticTranslationPreferences.LEGACY_GLOBAL_KEY,
                    true,
                    false,
                ),
            ),
        )
        val preferences = BookAutomaticTranslationPreferences(store)
        val document = preferences.automaticSelectionEnabled(DOCUMENT_SURFACE)
        val alternate = preferences.automaticSelectionEnabled(ALTERNATE_SURFACE)

        document.get() shouldBe true
        alternate.get() shouldBe true

        document.set(false)

        document.get() shouldBe false
        alternate.get() shouldBe true

        document.delete()

        document.get() shouldBe false
        alternate.get() shouldBe true
    }

    private companion object {
        const val DOCUMENT_SURFACE = "builtin.book.document"
        const val ALTERNATE_SURFACE = "test.book.alternate"
    }
}
